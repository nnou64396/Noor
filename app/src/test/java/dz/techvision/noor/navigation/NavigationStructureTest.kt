package dz.techvision.noor.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStructureTest {

    @Test
    fun bottomBar_hasExactlyFiveTabsInExpectedOrder() {
        assertEquals(
            listOf(NoorTab.HOME, NoorTab.QURAN, NoorTab.ATHKAR, NoorTab.QIBLA, NoorTab.SETTINGS),
            NoorTab.entries,
        )
    }

    @Test
    fun tabRoutes_areUnique() {
        val routes = NoorTab.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun tabRoutes_matchDestinationConstants() {
        assertEquals(NoorDestinations.HOME, NoorTab.HOME.route)
        assertEquals(NoorDestinations.QURAN, NoorTab.QURAN.route)
        assertEquals(NoorDestinations.ATHKAR, NoorTab.ATHKAR.route)
        assertEquals(NoorDestinations.QIBLA, NoorTab.QIBLA.route)
        assertEquals(NoorDestinations.SETTINGS, NoorTab.SETTINGS.route)
    }

    @Test
    fun destinationConstants_areNonEmptyAndDistinct() {
        val destinations = listOf(
            NoorDestinations.ONBOARDING,
            NoorDestinations.HOME,
            NoorDestinations.QURAN,
            NoorDestinations.QURAN_READER,
            NoorDestinations.PRAYER,
            NoorDestinations.ATHKAR,
            NoorDestinations.QIBLA,
            NoorDestinations.TASBIH,
            NoorDestinations.SETTINGS,
        )
        assertTrue(destinations.all { it.isNotBlank() })
        assertEquals(destinations.size, destinations.toSet().size)
    }

    @Test
    fun quranReaderRoute_encodesSurahNumber() {
        assertEquals("quran/surah/2", NoorDestinations.quranReaderRoute(2))
        assertEquals(
            "quran/surah/{surahNumber}",
            NoorDestinations.QURAN_READER,
        )
        assertFalse(NoorTab.QURAN.route == NoorDestinations.QURAN_READER)
    }
}

