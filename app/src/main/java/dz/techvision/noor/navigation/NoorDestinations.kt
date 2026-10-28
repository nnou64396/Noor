package dz.techvision.noor.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector
import dz.techvision.noor.R

/**
 * Stable route names used across the navigation graph.
 */
object NoorDestinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val QURAN = "quran"
    const val QURAN_READER = "quran/surah/{surahNumber}"
    const val QURAN_SURAH_ARG = "surahNumber"
    const val QURAN_AYAH_ARG = "ayahNumber"
    const val QURAN_SEARCH = "quran/search"
    const val QURAN_BOOKMARKS = "quran/bookmarks"
    const val ATHKAR_CATEGORY = "athkar/category/{categoryId}"
    const val ATHKAR_CATEGORY_ARG = "categoryId"
    const val PRAYER = "prayer"
    const val ATHKAR = "athkar"
    const val QIBLA = "qibla"
    const val TASBIH = "tasbih"
    const val SETTINGS = "settings"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_PERMISSIONS = "settings/permissions"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"

    fun quranReaderRoute(surahNumber: Int): String = "quran/surah/$surahNumber"

    /** Opens the adhkar of one category, by its corpus slug. */
    fun athkarCategoryRoute(categoryId: String): String = "athkar/category/$categoryId"

    /**
     * Opens the reader at one ayah. The surah stays in the path of the existing
     * reader route, so search results, bookmarks and Home all deep-link without
     * changing the route itself.
     */
    fun quranAyahRoute(surahNumber: Int, ayahNumberInSurah: Int): String =
        "${quranReaderRoute(surahNumber)}?$QURAN_AYAH_ARG=$ayahNumberInSurah"
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
