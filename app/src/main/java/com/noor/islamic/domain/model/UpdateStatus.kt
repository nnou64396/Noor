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
    data class VerificationFailed(val info: AppUpdateInfo, val reason: String) : UpdateStatus
    data class ReadyToInstall(val info: AppUpdateInfo) : UpdateStatus
    data class InstallPermissionRequired(val info: AppUpdateInfo) : UpdateStatus
    data class InstallFailed(val info: AppUpdateInfo, val reason: String) : UpdateStatus
}

/**
 * True when the update banner has a meaningful action or progress to show.
 *
 * The download, verification, and installation states must stay visible so the
 * user never loses progress or error feedback after tapping "Get Update".
 */
val UpdateStatus.isActionableUpdate: Boolean
    get() = this is UpdateStatus.UpdateAvailable ||
        this is UpdateStatus.UpdateRequired ||
        this is UpdateStatus.Downloading ||
        this is UpdateStatus.VerificationFailed ||
        this is UpdateStatus.ReadyToInstall ||
        this is UpdateStatus.InstallPermissionRequired ||
        this is UpdateStatus.InstallFailed
