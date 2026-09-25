package com.noor.islamic.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_settings"
)

/**
 * DataStore-backed implementation of [SettingsRepository].
 */
class SettingsDataStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    companion object {
        fun create(context: Context): SettingsDataStore =
            SettingsDataStore(context.noorDataStore)
    }

    override val language: Flow<Language> =
        dataStore.data.map { prefs ->
            Language.fromCode(prefs[Keys.LANGUAGE])
        }

    override val themeMode: Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            ThemeMode.fromStorageKey(prefs[Keys.THEME_MODE])
        }

    override val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] ?: false
        }

    override suspend fun setLanguage(language: Language) {
        dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.code }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = themeMode.storageKey }
    }

    override suspend fun setOnboardingComplete(completed: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETE] = completed }
    }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}