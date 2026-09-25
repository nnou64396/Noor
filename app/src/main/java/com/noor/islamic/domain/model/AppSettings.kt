package com.noor.islamic.domain.model

/**
 * Aggregate of the user preferences and flags that shape the app.
 */
data class AppSettings(
    val language: Language = Language.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val onboardingComplete: Boolean = false,
)