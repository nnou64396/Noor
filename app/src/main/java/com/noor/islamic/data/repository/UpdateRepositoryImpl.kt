package com.noor.islamic.data.repository

import android.content.Context
import com.noor.islamic.data.remote.update.UpdateApi
import com.noor.islamic.data.remote.update.UpdateManifestDto
import com.noor.islamic.data.update.ApkDownloader
import com.noor.islamic.data.update.ApkInstaller
import com.noor.islamic.data.update.ApkVerifier
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

val UpdateManifestDto.isValidOrParsable: Boolean
    get() = (versionName != null || latestVersion != null) && (versionCode != null || latestVersionCode != null)

class UpdateRepositoryImpl(
    private val context: Context,
    private val currentVersionCode: Long,
    private val settingsRepository: SettingsRepository,
    private val apkDownloader: ApkDownloader,
    private val apiProvider: () -> UpdateApi? = { null },
) : UpdateRepository {

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)
    override val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var downloadedApkFile: File? = null

    override suspend fun checkForUpdates(forceCheck: Boolean): UpdateStatus {
        val api = apiProvider()
        if (api == null) {
            _updateStatus.value = UpdateStatus.Disabled
            return UpdateStatus.Disabled
        }

        return try {
            val response = api.listReleases()
            if (!response.isSuccessful) {
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val releases = response.body() ?: emptyList()
            val latestRelease = releases.firstOrNull { rel ->
                rel.draft != true && rel.prerelease != true && !rel.tagName.isNullOrBlank()
            }

            if (latestRelease == null) {
                _updateStatus.value = UpdateStatus.UpToDate
                return UpdateStatus.UpToDate
            }

            val assets = latestRelease.assets ?: emptyList()
            val manifestAsset = assets.firstOrNull { it.name?.lowercase() == "noor-update.json" }
            var manifestDto: UpdateManifestDto? = null

            if (manifestAsset?.browserDownloadUrl != null) {
                val manifestRes = api.fetchManifest(manifestAsset.browserDownloadUrl)
                if (manifestRes.isSuccessful) {
                    manifestDto = manifestRes.body()
                }
            }

            val latestVersion: String
            val latestVersionCode: Long
            val minVersionCode: Long
            val mandatory: Boolean
            val sha256: String?
            val downloadUrl: String?
            val releaseNotes: Map<String, String>

            if (manifestDto != null && manifestDto.isValidOrParsable) {
                latestVersion = manifestDto.versionName ?: manifestDto.latestVersion ?: latestRelease.tagName!!.removePrefix("v")
                latestVersionCode = manifestDto.versionCode ?: manifestDto.latestVersionCode ?: parseVersionCode(latestVersion)
                minVersionCode = manifestDto.minimumSupportedVersionCode ?: 0L
                mandatory = manifestDto.mandatory == true || currentVersionCode < minVersionCode
                sha256 = manifestDto.sha256
                releaseNotes = manifestDto.releaseNotes ?: parseReleaseNotes(latestRelease.body)
                val apkAssetName = manifestDto.apkAsset ?: "app-release.apk"
                downloadUrl = manifestDto.downloadUrl ?: assets.firstOrNull { it.name == apkAssetName || it.name?.endsWith(".apk") == true }?.browserDownloadUrl
            } else {
                latestVersion = latestRelease.tagName!!.removePrefix("v")
                latestVersionCode = parseVersionCode(latestVersion)
                minVersionCode = 0L
                mandatory = false
                sha256 = null
                releaseNotes = parseReleaseNotes(latestRelease.body)
                downloadUrl = assets.firstOrNull { it.name?.endsWith(".apk") == true }?.browserDownloadUrl
            }

            if (latestVersionCode <= 0L || latestVersion.isBlank() || downloadUrl.isNullOrBlank()) {
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val info = AppUpdateInfo(
                latestVersion = latestVersion,
                latestVersionCode = latestVersionCode,
                minimumSupportedVersionCode = minVersionCode,
                mandatory = mandatory,
                sha256 = sha256,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                publishedAt = latestRelease.publishedAt,
            )

            val status = when {
                currentVersionCode < minVersionCode || mandatory -> UpdateStatus.UpdateRequired(info)
                currentVersionCode < latestVersionCode -> {
                    val dismissedCode = settingsRepository.getDismissedUpdateVersionCode()
                    if (dismissedCode == latestVersionCode && !forceCheck) {
                        UpdateStatus.UpToDate
                    } else {
                        UpdateStatus.UpdateAvailable(info)
                    }
                }
                else -> UpdateStatus.UpToDate
            }

            _updateStatus.value = status
            status
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            _updateStatus.value = UpdateStatus.CheckUnavailable
            UpdateStatus.CheckUnavailable
        }
    }

    override suspend fun downloadAndVerifyUpdate(info: AppUpdateInfo): Boolean {
        val url = info.downloadUrl ?: return false
        val expectedSha = info.sha256
        if (expectedSha.isNullOrBlank()) {
            _updateStatus.value = UpdateStatus.VerificationFailed("Missing required SHA-256 checksum in release metadata")
            return false
        }

        _downloadProgress.value = 0f
        _updateStatus.value = UpdateStatus.Downloading(0f)

        val result = apkDownloader.downloadApk(url) { progress ->
            _downloadProgress.value = progress
            _updateStatus.value = UpdateStatus.Downloading(progress)
        }

        if (!result.success || result.file == null || result.sha256Hex == null) {
            _updateStatus.value = UpdateStatus.VerificationFailed(result.error ?: "Download failed")
            return false
        }

        val verification = ApkVerifier.verifyApk(
            context = context,
            apkFile = result.file,
            expectedVersionCode = info.latestVersionCode,
            expectedSha256 = expectedSha,
            actualSha256 = result.sha256Hex,
        )

        if (!verification.success) {
            result.file.delete()
            _updateStatus.value = UpdateStatus.VerificationFailed(verification.error ?: "APK verification failed")
            return false
        }

        downloadedApkFile = result.file
        _updateStatus.value = UpdateStatus.ReadyToInstall(info)
        return true
    }

    override fun installUpdate(context: Context) {
        val file = downloadedApkFile
        if (file != null && file.exists()) {
            runCatching {
                ApkInstaller.installApk(context, file)
            }
        }
    }

    override suspend fun dismissUpdate(versionCode: Long) {
        settingsRepository.setDismissedUpdateVersionCode(versionCode)
        val current = _updateStatus.value
        if (current is UpdateStatus.UpdateAvailable && current.info.latestVersionCode == versionCode) {
            _updateStatus.value = UpdateStatus.UpToDate
        }
    }

    private fun parseVersionCode(versionName: String): Long {
        val parts = versionName.split(".")
        return runCatching {
            when (parts.size) {
                1 -> parts[0].toLong() * 10000L
                2 -> parts[0].toLong() * 10000L + parts[1].toLong() * 100L
                else -> parts[0].toLong() * 10000L + parts[1].toLong() * 100L + parts[2].toLong()
            }
        }.getOrDefault(1L)
    }

    private fun parseReleaseNotes(body: String?): Map<String, String> {
        if (body.isNullOrBlank()) return emptyMap()
        return mapOf("en" to body, "ar" to body, "fr" to body)
    }
}
