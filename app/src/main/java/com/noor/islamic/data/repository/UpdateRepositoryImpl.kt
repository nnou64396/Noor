package com.noor.islamic.data.repository

import com.noor.islamic.data.remote.update.UpdateApi
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Store-independent update repository implementation.
 *
 * Performs version checking against a remote JSON manifest URL if configured.
 * Safely handles missing endpoints, offline operation, and malformed responses
 * without throwing exceptions or blocking startup.
 */
class UpdateRepositoryImpl(
    private val currentVersionCode: Long,
    private val settingsRepository: SettingsRepository,
    private val manifestUrlProvider: () -> String? = { MANIFEST_URL },
    private val apiProvider: () -> UpdateApi? = { null },
) : UpdateRepository {

    companion object {
        /**
         * Default manifest URL. Set to null when unconfigured.
         * To enable remote updates, set this to a valid HTTPS URL hosting the manifest.
         */
        val MANIFEST_URL: String? = null
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.UpToDate)
    override val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    override suspend fun checkForUpdates(forceCheck: Boolean): UpdateStatus {
        val url = manifestUrlProvider()?.takeIf { it.isNotBlank() }
        if (url == null) {
            _updateStatus.value = UpdateStatus.Disabled
            return UpdateStatus.Disabled
        }

        val api = apiProvider()
        if (api == null) {
            _updateStatus.value = UpdateStatus.Disabled
            return UpdateStatus.Disabled
        }

        return try {
            val response = api.fetchManifest(url)
            if (!response.isSuccessful) {
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val body = response.body()
            if (body == null) {
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val latestVersion = body.latestVersion
            val latestVersionCode = body.latestVersionCode ?: 0L
            val minVersionCode = body.minimumSupportedVersionCode ?: 0L

            if (latestVersionCode <= 0L || latestVersion.isNullOrBlank()) {
                _updateStatus.value = UpdateStatus.CheckUnavailable
                return UpdateStatus.CheckUnavailable
            }

            val info = AppUpdateInfo(
                latestVersion = latestVersion,
                latestVersionCode = latestVersionCode,
                minimumSupportedVersionCode = minVersionCode,
                releaseNotes = body.releaseNotes ?: emptyMap(),
                downloadUrl = body.downloadUrl,
                publishedAt = body.publishedAt,
            )

            val status = when {
                currentVersionCode < minVersionCode -> UpdateStatus.UpdateRequired(info)
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

    override suspend fun dismissUpdate(versionCode: Long) {
        settingsRepository.setDismissedUpdateVersionCode(versionCode)
        val current = _updateStatus.value
        if (current is UpdateStatus.UpdateAvailable && current.info.latestVersionCode == versionCode) {
            _updateStatus.value = UpdateStatus.UpToDate
        }
    }
}
