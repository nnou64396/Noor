package com.noor.islamic.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
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
}
