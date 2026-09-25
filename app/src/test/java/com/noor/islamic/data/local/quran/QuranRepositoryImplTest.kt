package com.noor.islamic.data.local.quran

import com.noor.islamic.data.local.db.quran.AyahEntity
import com.noor.islamic.data.local.db.quran.QuranDao
import com.noor.islamic.data.local.db.quran.QuranEntityMapper
import com.noor.islamic.data.local.db.quran.SurahEntity
import com.noor.islamic.domain.model.quran.RevelationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranRepositoryImplTest {

    private class FakeDao : QuranDao {
        var lastSearchQuery: String? = null
        var lastSearchLimit: Int = 0
        var matches: List<AyahEntity> = emptyList()

        override fun observeSurahs(): Flow<List<SurahEntity>> = flowOf(emptyList())
        override suspend fun getSurah(surahNumber: Int): SurahEntity? = null
        override suspend fun getAyahsOfSurah(surahNumber: Int): List<AyahEntity> = emptyList()
        override suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): AyahEntity? = null
        override suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<AyahEntity> = emptyList()
        override suspend fun search(query: String, limit: Int): List<AyahEntity> {
            lastSearchQuery = query
            lastSearchLimit = limit
            return matches
        }
    }

    private fun ayah(
        global: Int,
        surah: Int,
        numInSurah: Int,
        text: String,
    ) = AyahEntity(
        globalAyahNumber = global,
        surahNumber = surah,
        ayahNumberInSurah = numInSurah,
        juzNumber = 1,
        pageNumber = 1,
        hasSajdah = false,
        textUthmani = text,
        textSearch = text,
    )

    @Test
    fun search_normalizesAndBuildsSafeFtsPrefixTerms() = runBlocking {
        val dao = FakeDao()
        val repo = QuranRepositoryImpl(dao, Dispatchers.Unconfined)
        dao.matches = listOf(ayah(1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))

        val result = repo.search("البِسْمِ اللَّهِ", limit = 7)

        assertEquals(1, result.size)
        assertEquals(7, dao.lastSearchLimit)
        assertEquals("البسم* AND الله*", dao.lastSearchQuery)
    }

    @Test
    fun search_dropsNonArabicAndNonLatinTokens() = runBlocking {
        val dao = FakeDao()
        val repo = QuranRepositoryImpl(dao, Dispatchers.Unconfined)

        repo.search("   !!!   ", limit = 5)

        assertEquals(dao.lastSearchQuery, null)
    }

    @Test
    fun search_filtersFtsReservedOperators() = runBlocking {
        val dao = FakeDao()
        val repo = QuranRepositoryImpl(dao, Dispatchers.Unconfined)
        dao.matches = emptyList()

        val result = repo.search("AND الرحمن", limit = 5)

        assertEquals(0, result.size)
        assertEquals("الرحمن*", dao.lastSearchQuery)
    }

    @Test
    fun mapping_roundTripsEntityToDomain() {
        val entity = SurahEntity(
            surahNumber = 2,
            nameArabic = "البقرة",
            nameTransliteration = "Al-Baqarah",
            nameEnglish = "The Cow",
            ayahCount = 286,
            revelationOrder = 87,
            revelationType = "MEDINAN",
            startPage = 2,
            basmala = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        )
        val domain = QuranEntityMapper.toDomain(entity)
        assertEquals(2, domain.surahNumber)
        assertEquals("Al-Baqarah", domain.nameTransliteration)
        assertEquals(RevelationType.MEDINAN, domain.revelationType)
        assertEquals(2, domain.startPage)
        assertEquals(entity.basmala, domain.basmala)

        val ayahDomain = QuranEntityMapper.toDomain(ayah(42, 2, 1, "الٓمٓ"))
        assertEquals(42, ayahDomain.globalAyahNumber)
        assertEquals(2, ayahDomain.surahNumber)
        assertTrue(ayahDomain.textUthmani.startsWith("الٓمٓ"))
    }
}