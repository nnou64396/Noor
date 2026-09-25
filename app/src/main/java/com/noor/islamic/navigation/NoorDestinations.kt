package com.noor.islamic.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.noor.islamic.R

/**
 * Stable route names used across the navigation graph.
 */
object NoorDestinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val QURAN = "quran"
    const val QURAN_READER = "quran/surah/{surahNumber}"
    const val QURAN_SURAH_ARG = "surahNumber"
    const val QURAN_SEARCH = "quran/search"
    const val QURAN_BOOKMARKS = "quran/bookmarks"
    const val PRAYER = "prayer"
    const val ATHKAR = "athkar"
    const val QIBLA = "qibla"
    const val TASBIH = "tasbih"
    const val SETTINGS = "settings"

    fun quranReaderRoute(surahNumber: Int): String = "quran/surah/$surahNumber"

    /**
     * Opens the reader at one ayah. The existing surah route is left untouched so
     * bookmarks, search results and Home all deep-link without changing it.
     */
    fun quranAyahRoute(surahNumber: Int, ayahNumberInSurah: Int): String =
        "$QURAN_READER?$QURAN_SURAH_ARG=$surahNumber&$QURAN_AYAH_ARG=$ayahNumberInSurah"

    const val QURAN_AYAH_ARG = "ayahNumber"
}

/**
 * Destinations shown in the bottom navigation bar.
 */
enum class NoorTab(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(NoorDestinations.HOME, R.string.nav_home, Icons.Rounded.Home),
    QURAN(NoorDestinations.QURAN, R.string.nav_quran, Icons.AutoMirrored.Rounded.MenuBook),
    ATHKAR(NoorDestinations.ATHKAR, R.string.nav_athkar, Icons.Rounded.Star),
    QIBLA(NoorDestinations.QIBLA, R.string.nav_qibla, Icons.Rounded.Explore),
    SETTINGS(NoorDestinations.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings);

    companion object {
        val allRoutes: Set<String> = entries.map { it.route }.toSet()
    }
}