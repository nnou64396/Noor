package com.noor.islamic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noor.islamic.data.local.db.quran.QuranReaderDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the prebuilt `assets/quran/quran_reader.db` opens through Room's
 * own validation (identity hash, schema version, table shapes) and serves
 * real reads: surah metadata, ayah ranges and FTS4 prefix search.
 */
@RunWith(AndroidJUnit4::class)
class QuranAssetDatabaseTest {

    @Test
    fun prebuiltAssetOpens_andServesReads() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = QuranReaderDatabase.create(context)
        try {
            val dao = db.quranDao()

            val surahs = dao.observeSurahs().first()
            assertEquals(114, surahs.size)
            assertEquals("الفاتحة", surahs[0].nameArabic)
            assertNotNull(surahs[1].basmala)
            assertEquals(null, surahs[0].basmala)
            assertEquals(null, surahs[8].basmala)

            val baqarah = dao.getAyahsOfSurah(2)
            assertEquals(286, baqarah.size)
            assertEquals(1, baqarah[0].ayahNumberInSurah)
            assertEquals("بِسْمِ", baqarah[0].textUthmani.take("بِسْمِ".length))

            val ayatAlKursi = dao.getAyah(2, 255)
            assertNotNull(ayatAlKursi)
            assertEquals(255, requireNotNull(ayatAlKursi).ayahNumberInSurah)

            val page2 = dao.getAyahRange(8, 8)
            assertEquals(1, page2.size)
            assertEquals(2, page2[0].surahNumber)
            assertEquals(1, page2[0].ayahNumberInSurah)
            assertEquals(2, page2[0].pageNumber)

            val hits = dao.search("الرحمن*", 5)
            assertTrue("FTS must answer prefix queries", hits.isNotEmpty())
            assertTrue(hits.size <= 5)
        } finally {
            db.close()
        }
    }
}