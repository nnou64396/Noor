package dz.techvision.noor.domain.model

import androidx.annotation.StringRes
import dz.techvision.noor.R

/**
 * Appearance mode of the application.
 */
enum class ThemeMode(
    val storageKey: String,
    @param:StringRes val displayNameRes: Int,
) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    companion object {
        val DEFAULT: ThemeMode = SYSTEM

        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
