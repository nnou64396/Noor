package dz.techvision.noor.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dz.techvision.noor.services.ServiceLocator

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

