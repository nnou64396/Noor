package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for persisted user preferences.
 */
interface SettingsRepository {
    val language: Flow<Language>
    val themeMode: Flow<ThemeMode>
    val onboardingComplete: Flow<Boolean>

    suspend fun setLanguage(language: Language)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setOnboardingComplete(completed: Boolean)
}