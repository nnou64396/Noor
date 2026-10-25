package com.noor.islamic.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.noor.islamic.services.ServiceLocator

/**
 * WorkManager worker for performing background periodic update checks.
 */
class UpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = ServiceLocator.updateRepository
            repository.checkForUpdates(forceCheck = false)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
