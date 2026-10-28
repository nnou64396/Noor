package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for persisted user preferences.
 */
interface SettingsRepository {
    val language: Flow<Language>
    val themeMode: Flow<ThemeMode>
    val onboardingComplete: Flow<Boolean>
    val prayerCalculationMethod: Flow<CalculationMethod>
    val prayerAsrSchool: Flow<AsrSchool>
    val prayerLocationMode: Flow<LocationMode>
    val prayerManualLatitude: Flow<Double?>
    val prayerManualLongitude: Flow<Double?>
    val notificationsEnabled: Flow<Boolean>
    val prayerRemindersEnabled: Flow<Boolean>
    val updateNotificationsEnabled: Flow<Boolean>

    suspend fun setLanguage(language: Language)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setOnboardingComplete(completed: Boolean)
    suspend fun setPrayerCalculationMethod(method: CalculationMethod)
    suspend fun setPrayerAsrSchool(school: AsrSchool)
    suspend fun setPrayerLocationMode(mode: LocationMode)
    suspend fun setPrayerManualLocation(latitude: Double, longitude: Double)
    suspend fun clearPrayerManualLocation()
    suspend fun getDismissedUpdateVersionCode(): Long
    suspend fun setDismissedUpdateVersionCode(code: Long)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setPrayerRemindersEnabled(enabled: Boolean)
    suspend fun setUpdateNotificationsEnabled(enabled: Boolean)
}

