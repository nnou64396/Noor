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
    }
}
