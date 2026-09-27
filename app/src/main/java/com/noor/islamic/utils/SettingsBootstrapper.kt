package com.noor.islamic.utils

import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.services.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Restores the persisted language and theme before the first frame.
 *
 * `SettingsDataStore` remains the single source of truth; this only reads it
 * once at process start and hands the values to the AppCompat delegates, which
 * is what actually makes the XML theme and the resource configuration match the
 * stored choice. Without this, a process restart would render with the system
 * locale and system night mode and only visibly switch later, once
 * [com.noor.islamic.presentation.MainViewModel] observed the stored value.
 *
 * Called from `Application.onCreate`, so it runs before any Activity exists and
 * therefore cannot flash the wrong theme.
 */
object SettingsBootstrapper {

    /**
     * Blocking by design: the stored values must be applied before the first
     * Activity is created. The read is one small preferences file.
     */
    fun restorePersistedSettings() {
        val settings = readPersistedSettings()
        LocaleManager.apply(settings.language)
        ThemeManager.apply(settings.themeMode)
    }

    /**
     * The stored settings exactly as a fresh process start would observe them.
     * Exposed separately so the restoration contract can be tested.
     */
    fun readPersistedSettings(): AppSettings = runBlocking {
        ServiceLocator.observeSettings.invoke().first()
    }
}
