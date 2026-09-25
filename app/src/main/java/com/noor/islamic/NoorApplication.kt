package com.noor.islamic

import android.app.Application
import com.noor.islamic.services.ServiceLocator

class NoorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Locales are persisted in DataStore and applied manually, so
        // AndroidX auto-storage is disabled via the manifest service entry.
        ServiceLocator.init(this)
    }
}