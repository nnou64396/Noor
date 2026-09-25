package com.noor.islamic.utils

import androidx.appcompat.app.AppCompatDelegate
import com.noor.islamic.domain.model.ThemeMode

/**
 * Applies the persisted theme mode to the whole application.
 */
object ThemeManager {

    fun apply(themeMode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(themeMode.toAppCompatNightMode())
    }

    fun ThemeMode.toAppCompatNightMode(): Int = when (this) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
}