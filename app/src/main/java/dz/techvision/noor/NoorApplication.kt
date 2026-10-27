package dz.techvision.noor

import android.app.Application
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.utils.SettingsBootstrapper

class NoorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Locales are persisted in DataStore and applied manually, so
        // AndroidX auto-storage is disabled via the manifest service entry.
        ServiceLocator.init(this)
        // Apply the stored language and night mode before the first Activity is
        // created, otherwise a process restart would start on the system
        // locale/theme and visibly switch afterwards.
        SettingsBootstrapper.restorePersistedSettings()

        // Schedule periodic background update checks
        runCatching {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val updateRequest = androidx.work.PeriodicWorkRequestBuilder<dz.techvision.noor.data.update.UpdateWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "NoorBackgroundUpdateCheck",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                updateRequest,
            )
        }
    }
}
