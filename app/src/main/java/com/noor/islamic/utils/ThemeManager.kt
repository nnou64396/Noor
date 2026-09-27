package com.noor.islamic.utils

import androidx.appcompat.app.AppCompatDelegate
import com.noor.islamic.domain.model.ThemeMode

/**
 * Applies the persisted theme mode to the whole application.
 */
object ThemeManager {

    /**
     * `setDefaultNightMode` recreates every started Activity, so the call is
     * skipped when the stored mode is already in effect. This is what keeps the
     * startup restore from forcing a needless recreate on every cold start.
     */
    fun apply(themeMode: ThemeMode) {
        val target = themeMode.toAppCompatNightMode()
        if (AppCompatDelegate.getDefaultNightMode() == target) return
        AppCompatDelegate.setDefaultNightMode(target)
    }

    fun ThemeMode.toAppCompatNightMode(): Int = when (this) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }
}
