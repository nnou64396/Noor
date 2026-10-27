package dz.techvision.noor.data.local.db.quran

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Opens the prebuilt `assets/quran/quran_reader.db` through Room's own runtime
 * validation on the JVM (Robolectric), so the "does the asset open as a real Room
 * database" check runs without a physical device. Mirrors QuranAssetDatabaseTest,
 * which exercises the same path on hardware.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuranAssetDatabaseRobolectricTest {

    @Test
    fun prebuiltAssetOpens_andServesReads() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
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

            // FTS MATCH is intentionally not asserted here: Robolectric's bundled SQLite
            // tokenizes differently (and its FTS3/4 build varies); the same query hits on
            // the xerial build (importer test) and on Android's SQLite (device test).
        } finally {
            db.close()
        }
    }
}
