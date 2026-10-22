package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.UpdateStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for checking and managing application updates.
 */
interface UpdateRepository {
    val updateStatus: StateFlow<UpdateStatus>

    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateStatus
    suspend fun dismissUpdate(versionCode: Long)
}
