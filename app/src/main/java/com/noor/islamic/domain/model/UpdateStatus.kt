package com.noor.islamic.domain.model

/**
 * State representing the status of an update check.
 */
sealed interface UpdateStatus {
    /** Application is up to date (current versionCode >= latestVersionCode). */
    data object UpToDate : UpdateStatus

    /** An optional update is available (latestVersionCode > current, and current >= minimumSupportedVersionCode). */
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus

    /** A recommended or required update is available (current versionCode < minimumSupportedVersionCode). */
    data class UpdateRequired(val info: AppUpdateInfo) : UpdateStatus

    /** Update checking is disabled or unconfigured. */
    data object Disabled : UpdateStatus

    /** Update check failed due to network, offline state, or malformed manifest. */
    data object CheckUnavailable : UpdateStatus
}
