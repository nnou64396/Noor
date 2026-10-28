package dz.techvision.noor.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    override val prayerCalculationMethod: Flow<CalculationMethod> =
        dataStore.data.map { prefs ->
            CalculationMethod.fromStorageKey(prefs[Keys.PRAYER_CALCULATION_METHOD])
        }

    override val prayerAsrSchool: Flow<AsrSchool> =
        dataStore.data.map { prefs ->
            AsrSchool.fromStorageKey(prefs[Keys.PRAYER_ASR_SCHOOL])
        }

    override val prayerLocationMode: Flow<LocationMode> =
        dataStore.data.map { prefs ->
            LocationMode.fromStorageKey(prefs[Keys.PRAYER_LOCATION_MODE])
        }

    override val prayerManualLatitude: Flow<Double?> =
        dataStore.data.map { prefs ->
            prefs[Keys.PRAYER_MANUAL_LATITUDE]?.takeIf { it.isFinite() && it in -90.0..90.0 }
        }

    override val prayerManualLongitude: Flow<Double?> =
        dataStore.data.map { prefs ->
            prefs[Keys.PRAYER_MANUAL_LONGITUDE]?.takeIf { it.isFinite() && it in -180.0..180.0 }
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

    override suspend fun setPrayerCalculationMethod(method: CalculationMethod) {
        dataStore.edit { prefs -> prefs[Keys.PRAYER_CALCULATION_METHOD] = method.storageKey }
    }

    override suspend fun setPrayerAsrSchool(school: AsrSchool) {
        dataStore.edit { prefs -> prefs[Keys.PRAYER_ASR_SCHOOL] = school.storageKey }
    }

    override suspend fun setPrayerLocationMode(mode: LocationMode) {
        dataStore.edit { prefs -> prefs[Keys.PRAYER_LOCATION_MODE] = mode.storageKey }
    }

    override suspend fun setPrayerManualLocation(latitude: Double, longitude: Double) {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        dataStore.edit { prefs ->
            prefs[Keys.PRAYER_MANUAL_LATITUDE] = latitude
            prefs[Keys.PRAYER_MANUAL_LONGITUDE] = longitude
        }
    }

    override suspend fun clearPrayerManualLocation() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PRAYER_MANUAL_LATITUDE)
            prefs.remove(Keys.PRAYER_MANUAL_LONGITUDE)
        }
    }

    override suspend fun getDismissedUpdateVersionCode(): Long {
        return dataStore.data.first()[Keys.DISMISSED_UPDATE_VERSION_CODE] ?: 0L
    }

    override suspend fun setDismissedUpdateVersionCode(code: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_UPDATE_VERSION_CODE] = code
        }
    }

    override val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        }

    override val prayerRemindersEnabled: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.PRAYER_REMINDERS_ENABLED] ?: true
        }

    override val updateNotificationsEnabled: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[Keys.UPDATE_NOTIFICATIONS_ENABLED] ?: true
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setPrayerRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.PRAYER_REMINDERS_ENABLED] = enabled }
    }

    override suspend fun setUpdateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.UPDATE_NOTIFICATIONS_ENABLED] = enabled }
    }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PRAYER_CALCULATION_METHOD = stringPreferencesKey("prayer_calculation_method")
        val PRAYER_ASR_SCHOOL = stringPreferencesKey("prayer_asr_school")
        val PRAYER_LOCATION_MODE = stringPreferencesKey("prayer_location_mode")
        val PRAYER_MANUAL_LATITUDE = doublePreferencesKey("prayer_manual_latitude")
        val PRAYER_MANUAL_LONGITUDE = doublePreferencesKey("prayer_manual_longitude")
        val DISMISSED_UPDATE_VERSION_CODE = longPreferencesKey("dismissed_update_version_code")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PRAYER_REMINDERS_ENABLED = booleanPreferencesKey("prayer_reminders_enabled")
        val UPDATE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("update_notifications_enabled")
    }
}

