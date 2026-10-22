package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.LocationMode
import com.noor.islamic.domain.model.ThemeMode
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
}
