package com.noor.islamic.domain.model

/**
 * State representing the status of an update check, download, and verification flow.
 */
sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus
    data class UpdateRequired(val info: AppUpdateInfo) : UpdateStatus
    data object Disabled : UpdateStatus
    data object CheckUnavailable : UpdateStatus
    data class Downloading(val progress: Float) : UpdateStatus
    data class VerificationFailed(val reason: String) : UpdateStatus
    data class ReadyToInstall(val info: AppUpdateInfo) : UpdateStatus
}
