package dz.techvision.noor.data.repository

import android.content.Context
import dz.techvision.noor.data.remote.update.UpdateApi
import dz.techvision.noor.data.remote.update.UpdateManifestDto
import dz.techvision.noor.data.update.ApkDownloadService
import dz.techvision.noor.data.update.ApkInstaller
import dz.techvision.noor.data.update.ApkInstallerService
import dz.techvision.noor.data.update.ApkVerificationService
import dz.techvision.noor.data.update.ApkVerifier
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.repository.SettingsRepository
import dz.techvision.noor.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

val UpdateManifestDto.isValidOrParsable: Boolean
    get() = (versionName != null || latestVersion != null) && (versionCode != null || latestVersionCode != null)

class UpdateRepositoryImpl(
    private val context: Context,
    private val currentVersionCode: Long,
    private val settingsRepository: SettingsRepository,
    private val apkDownloader: ApkDownloadService,
    private val apkVerifier: ApkVerificationService = ApkVerifier,
    private val apkInstaller: ApkInstallerService = ApkInstaller,
    private val apiProvider: () -> UpdateApi? = { null },
) : UpdateRepository {

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)
    override val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val downloadMutex = Mutex()
    private var downloadedApkFile: File? = null
    private var activeUpdateInfo: AppUpdateInfo? = null

    override suspend fun checkForUpdates(forceCheck: Boolean): UpdateStatus {
        // A check must never clobber an active download's progress.
        val statusBeforeCheck = _updateStatus.value
        if (statusBeforeCheck is UpdateStatus.Downloading) {
            return statusBeforeCheck
        }

        val api = apiProvider()
        if (api == null) {
            _updateStatus.value = UpdateStatus.Disabled
            return UpdateStatus.Disabled
        }

        return try {
            val response = api.listReleases()
            if (!response.isSuccessful) {
                android.util.Log.w("UpdateRepository", "Failed to list releases: HTTP ${response.code()} ${response.message()}")
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val releases = response.body() ?: emptyList()
            val latestRelease = releases.firstOrNull { rel ->
                rel.draft != true && rel.prerelease != true && !rel.tagName.isNullOrBlank()
            }

            if (latestRelease == null) {
                clearPreparedUpdate()
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

            if (activeUpdateInfo?.latestVersionCode != info.latestVersionCode) {
                clearPreparedUpdate()
                activeUpdateInfo = info
            }

            // Do not throw away a verified APK or an actionable install state
            // just because the periodic check ran again.
            val preparedStatus = _updateStatus.value
            if (preparedStatus.isPreparedFor(info)) {
                return preparedStatus
            }

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

            if (status == UpdateStatus.UpToDate) {
                clearPreparedUpdate()
            }
            _updateStatus.value = status
            status
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            android.util.Log.e("UpdateRepository", "Error checking for updates", t)
            _updateStatus.value = UpdateStatus.CheckUnavailable
            UpdateStatus.CheckUnavailable
        }
    }

    override suspend fun downloadAndVerifyUpdate(info: AppUpdateInfo): Boolean {
        // Repeated taps while a download is active must not start a second one.
        if (!downloadMutex.tryLock()) {
            return false
        }

        try {
            return performDownloadAndVerify(info)
        } finally {
            downloadMutex.unlock()
        }
    }

    private suspend fun performDownloadAndVerify(info: AppUpdateInfo): Boolean {
        val url = info.downloadUrl
        if (url.isNullOrBlank()) {
            _updateStatus.value = UpdateStatus.VerificationFailed(
                info,
                "The update download address is missing. Please try again later.",
            )
            return false
        }

        val expectedSha = info.sha256
        if (expectedSha.isNullOrBlank()) {
            _updateStatus.value = UpdateStatus.VerificationFailed(
                info,
                "The update metadata is missing its required SHA-256 checksum, so installation was blocked.",
            )
            return false
        }

        if (activeUpdateInfo?.latestVersionCode != info.latestVersionCode) {
            clearPreparedUpdate()
        }
        activeUpdateInfo = info

        _downloadProgress.value = 0f
        _updateStatus.value = UpdateStatus.Downloading(0f)

        var candidateFile: File? = null
        try {
            val result = apkDownloader.downloadApk(url) { progress ->
                _downloadProgress.value = progress
                _updateStatus.value = UpdateStatus.Downloading(progress)
            }

            if (!result.success) {
                _downloadProgress.value = 0f
                _updateStatus.value = UpdateStatus.VerificationFailed(
                    info,
                    downloadFailureMessage(result.failure),
                )
                return false
            }

            val file = result.file ?: return false
            val actualSha = result.sha256Hex ?: return false
            candidateFile = file

            // Package parsing, certificate extraction, and hashing are blocking
            // operations and must never run on the main dispatcher.
            val verification = withContext(Dispatchers.IO) {
                apkVerifier.verifyApk(
                    context = context,
                    apkFile = file,
                    expectedVersionCode = info.latestVersionCode,
                    expectedSha256 = expectedSha,
                    actualSha256 = actualSha,
                )
            }

            if (!verification.success) {
                runCatching { file.delete() }
                candidateFile = null
                downloadedApkFile = null
                _downloadProgress.value = 0f
                _updateStatus.value = UpdateStatus.VerificationFailed(
                    info,
                    verification.error ?: "The downloaded update failed security verification.",
                )
                return false
            }

            downloadedApkFile = file
            _downloadProgress.value = 1f
            _updateStatus.value = UpdateStatus.ReadyToInstall(info)
            return true
        } catch (e: CancellationException) {
            candidateFile?.let { runCatching { it.delete() } }
            _downloadProgress.value = 0f
            if (_updateStatus.value is UpdateStatus.Downloading) {
                _updateStatus.value = recoverableStatus(info)
            }
            throw e
        } catch (t: Throwable) {
            candidateFile?.let { runCatching { it.delete() } }
            downloadedApkFile = null
            _downloadProgress.value = 0f
            _updateStatus.value = UpdateStatus.VerificationFailed(
                info,
                "The update could not be verified. Please try again.",
            )
            return false
        }
    }

    override fun installUpdate(context: Context) {
        val info = activeUpdateInfo ?: actionableInfo() ?: return
        val file = downloadedApkFile

        if (file == null || !file.exists()) {
            downloadedApkFile = null
            _updateStatus.value = UpdateStatus.InstallFailed(
                info,
                "The verified update file is no longer available. Please download the update again.",
            )
            return
        }

        if (!apkInstaller.canRequestPackageInstalls(context)) {
            _updateStatus.value = UpdateStatus.InstallPermissionRequired(info)
            return
        }

        when (val outcome = apkInstaller.installApk(context, file)) {
            is ApkInstaller.InstallOutcome.Launched -> {
                // The installer is now in front. We cannot know whether the user
                // completes it, so the verified state remains available on return.
                _updateStatus.value = UpdateStatus.ReadyToInstall(info)
            }
            is ApkInstaller.InstallOutcome.PermissionRequired -> {
                _updateStatus.value = UpdateStatus.InstallPermissionRequired(info)
            }
            is ApkInstaller.InstallOutcome.Failure -> {
                _updateStatus.value = UpdateStatus.InstallFailed(info, outcome.reason)
            }
        }
    }

    override fun onInstallPermissionResult(context: Context): Boolean {
        val info = activeUpdateInfo ?: actionableInfo() ?: return false
        if (!apkInstaller.canRequestPackageInstalls(context)) {
            _updateStatus.value = UpdateStatus.InstallPermissionRequired(info)
            return false
        }

        installUpdate(context)
        return true
    }

    override suspend fun retryUpdate(info: AppUpdateInfo, context: Context) {
        val file = downloadedApkFile
        if (file != null && file.exists() && activeUpdateInfo?.latestVersionCode == info.latestVersionCode) {
            installUpdate(context)
        } else {
            downloadAndVerifyUpdate(info)
        }
    }

    override suspend fun dismissUpdate(versionCode: Long) {
        // Dismissal is a UI preference, never a cancellation mechanism for an
        // operation the user has already started.
        if (_updateStatus.value is UpdateStatus.Downloading) {
            return
        }
        settingsRepository.setDismissedUpdateVersionCode(versionCode)
        val current = _updateStatus.value
        if (current is UpdateStatus.UpdateAvailable && current.info.latestVersionCode == versionCode) {
            _updateStatus.value = UpdateStatus.UpToDate
        }
    }

    private fun UpdateStatus.isPreparedFor(info: AppUpdateInfo): Boolean = when (this) {
        is UpdateStatus.ReadyToInstall ->
            this.info.latestVersionCode == info.latestVersionCode && downloadedApkFile?.exists() == true
        is UpdateStatus.InstallPermissionRequired -> this.info.latestVersionCode == info.latestVersionCode
        is UpdateStatus.InstallFailed -> this.info.latestVersionCode == info.latestVersionCode
        else -> false
    }

    private fun actionableInfo(): AppUpdateInfo? = when (val status = _updateStatus.value) {
        is UpdateStatus.UpdateAvailable -> status.info
        is UpdateStatus.UpdateRequired -> status.info
        is UpdateStatus.VerificationFailed -> status.info
        is UpdateStatus.ReadyToInstall -> status.info
        is UpdateStatus.InstallPermissionRequired -> status.info
        is UpdateStatus.InstallFailed -> status.info
        else -> null
    }

    private fun recoverableStatus(info: AppUpdateInfo): UpdateStatus =
        if (info.mandatory || currentVersionCode < info.minimumSupportedVersionCode) {
            UpdateStatus.UpdateRequired(info)
        } else {
            UpdateStatus.UpdateAvailable(info)
        }

    private fun clearPreparedUpdate() {
        downloadedApkFile?.let { file -> runCatching { file.delete() } }
        downloadedApkFile = null
        activeUpdateInfo = null
    }

    private fun downloadFailureMessage(failure: ApkDownloadService.Failure?): String = when (failure) {
        is ApkDownloadService.Failure.InvalidUrl,
        is ApkDownloadService.Failure.UntrustedHost,
        -> "The update download address is invalid or not trusted."
        is ApkDownloadService.Failure.Http ->
            "The update server returned an error (HTTP ${failure.code}). Please try again later."
        is ApkDownloadService.Failure.EmptyResponse ->
            "The update server returned an empty file. Please try again later."
        is ApkDownloadService.Failure.TooLarge ->
            "The update file is larger than the allowed 50 MB limit."
        is ApkDownloadService.Failure.FileAccess ->
            "The update file could not be saved. Check available storage and try again."
        is ApkDownloadService.Failure.Network,
        null,
        -> "The update could not be downloaded. Check your connection and try again."
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

