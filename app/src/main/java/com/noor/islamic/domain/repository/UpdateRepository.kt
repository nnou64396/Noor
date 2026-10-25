package com.noor.islamic.domain.repository

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
    fun installUpdate(context: android.content.Context)
    suspend fun dismissUpdate(versionCode: Long)
}
