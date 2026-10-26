package com.noor.islamic.domain.repository

import android.content.Context
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.UpdateStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for checking, downloading, verifying, and installing application updates.
 */
interface UpdateRepository {
    val updateStatus: StateFlow<UpdateStatus>
    val downloadProgress: StateFlow<Float>

    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateStatus
    suspend fun downloadAndVerifyUpdate(info: AppUpdateInfo): Boolean

    /** Launches installation, or moves to the permission/failure state when needed. */
    fun installUpdate(context: Context)

    /**
     * Re-checks the unknown-app-sources permission after returning from system
     * settings. Returns true when installation was resumed.
     */
    fun onInstallPermissionResult(context: Context): Boolean

    /** Real retry: reinstall a still-valid APK, otherwise download it again. */
    suspend fun retryUpdate(info: AppUpdateInfo, context: Context)

    suspend fun dismissUpdate(versionCode: Long)
}
