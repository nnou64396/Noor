package com.noor.quran.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranSequencingTest {

    private val ayahs = listOf(
        QuranAyah(1, 1, "a1"),
        QuranAyah(1, 2, "a2"),
        QuranAyah(2, 1, "b1"),
        QuranAyah(2, 2, "b2"),
        QuranAyah(9, 1, "i1"),
        QuranAyah(9, 2, "i2"),
    )

    private fun metadata() = QuranMetadata(
        surahs = listOf(
            SurahMeta(1, 2, 0, "x", "x", "x", RevelationType.MECCAN, 5, 0),
            SurahMeta(2, 2, 2, "x", "x", "x", RevelationType.MEDINAN, 87, 0),
            SurahMeta(9, 2, 4, "x", "x", "x", RevelationType.MEDINAN, 113, 0),
        ),
        juz = listOf(
            JuzBoundary(1, AyahRef(1, 1)),
            JuzBoundary(2, AyahRef(2, 1)),
            JuzBoundary(3, AyahRef(9, 2)),
        ),
        pages = listOf(
            PageBoundary(1, AyahRef(1, 1)),
            PageBoundary(2, AyahRef(1, 2)),
            PageBoundary(3, AyahRef(9, 1)),
        ),
        sajdas = listOf(AyahRef(2, 2), AyahRef(9, 2)),
    )

    @Test
    fun assignsSequentialGlobalNumbers() {
        val attrs = QuranSequencing.computeAttributes(ayahs, metadata())
        assertEquals((1..6).toList(), attrs.map { it.globalAyahNumber })
    }

    @Test
    fun assignsJuzAndPageFromBoundaries() {
        val attrs = QuranSequencing.computeAttributes(ayahs, metadata())
        assertEquals(listOf(1, 1, 2, 2, 2, 3), attrs.map { it.juz })
        assertEquals(listOf(1, 2, 2, 2, 3, 3), attrs.map { it.pageNumber })
    }

    @Test
    fun flagsSajdahAyatOnlyAtReferences() {
        val attrs = QuranSequencing.computeAttributes(ayahs, metadata())
        assertEquals(listOf(false, false, false, true, false, true), attrs.map { it.hasSajdah })
    }

    @Test
    fun computesSurahStartPages() {
        val attrs = QuranSequencing.computeAttributes(ayahs, metadata())
        val pages = QuranSequencing.surahStartPages(attrs)
        assertEquals(1, pages[1])
        assertEquals(2, pages[2])
        assertEquals(3, pages[9])
    }

    @Test
    fun displayTextMergesBasmalaForAyahOneOnly() {
        val text = QuranText(
            listOf(
                SurahText(1, "الف", null, listOf(QuranAyah(1, 1, "basmala and 1:1"))),
                SurahText(2, "البقرة", "بسم الله", listOf(QuranAyah(2, 1, "الم") , QuranAyah(2, 2, "ذالك"))),
            ),
        )
        assertEquals("basmala and 1:1", text.displayText(1, 1))
        assertEquals("بسم الله الم", text.displayText(2, 1))
        assertEquals("ذالك", text.displayText(2, 2))
        assertTrue(SurahText.hasSeparateBasmala(2))
        assertFalse(SurahText.hasSeparateBasmala(1))
        assertFalse(SurahText.hasSeparateBasmala(9))
    }
}