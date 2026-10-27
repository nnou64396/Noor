package dz.techvision.noor.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reader route carries the surah in its path and accepts an optional ayah
 * in the query, so search results, bookmarks and Home can land on one ayah.
 * These tests parse the generated URIs the same way `NoorNavHost` reads them, so
 * a leftover path placeholder fails here instead of at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoorDestinationsTest {

    @Test
    fun readerRoute_keepsTheExistingPath() {
        assertEquals("quran/surah/{surahNumber}", NoorDestinations.QURAN_READER)
        assertEquals("quran/surah/18", NoorDestinations.quranReaderRoute(18))
    }

    @Test
    fun readerRoute_putsTheSurahInThePathAndLeavesTheAyahUnset() {
        val uri = Uri.parse(NoorDestinations.quranReaderRoute(18))

        assertEquals(listOf("quran", "surah", "18"), uri.pathSegments)
        assertEquals("18", uri.pathSegments.last())
        assertNull(uri.getQueryParameter(NoorDestinations.QURAN_AYAH_ARG))
    }

    @Test
    fun ayahRoute_keepsTheSurahInThePathAndAddsTheAyahToTheQuery() {
        val uri = Uri.parse(NoorDestinations.quranAyahRoute(2, 255))

        assertEquals("2", uri.pathSegments.last())
        assertEquals("255", uri.getQueryParameter(NoorDestinations.QURAN_AYAH_ARG))
    }

    @Test
    fun ayahRoute_neverLeaksAPlaceholderIntoTheUri() {
        val route = NoorDestinations.quranAyahRoute(2, 255)

        assertTrue(route.none { it == '{' || it == '}' })
        assertTrue(route.startsWith(NoorDestinations.quranReaderRoute(2)))
        assertEquals("2", Uri.parse(route).lastPathSegment)
    }

    @Test
    fun athkarCategoryRoute_keepsTheCategoryInThePath() {
        assertEquals("athkar/category/{categoryId}", NoorDestinations.ATHKAR_CATEGORY)
        assertEquals("categoryId", NoorDestinations.ATHKAR_CATEGORY_ARG)
        assertEquals("athkar/category/morning", NoorDestinations.athkarCategoryRoute("morning"))
    }

    @Test
    fun athkarCategoryRoute_neverLeaksAPlaceholderIntoTheUri() {
        val route = NoorDestinations.athkarCategoryRoute("after_prayer")

        assertTrue(route.none { it == '{' || it == '}' })
        assertEquals("after_prayer", Uri.parse(route).lastPathSegment)
        assertTrue(route.startsWith(NoorDestinations.ATHKAR))
    }

    @Test
    fun everyTabRoute_isAUniqueDestination() {
        val routes = NoorTab.entries.map { it.route }

        assertEquals(routes.size, routes.toSet().size)
        assertTrue(NoorDestinations.ATHKAR in routes)
    }

    @Test
    fun tasbih_isADestinationButNotATab() {
        assertEquals("tasbih", NoorDestinations.TASBIH)
        // Tasbih is reached from Home, so it must not collide with a tab route.
        assertFalse(NoorDestinations.TASBIH in NoorTab.allRoutes)
    }
}

