package com.noor.islamic.presentation.quran

import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.RevelationType
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranReaderViewModelTest {

    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun readyState_loadsRequestedSurahAndPreservesAyahOrderAndText() = runTest(mainDispatcher) {
        val exactText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ الٓمٓ"
        val surah = testReaderSurah(2)
        val ayahs = listOf(
            testAyah(7, 2, 1, exactText),
            testAyah(8, 2, 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"),
            testAyah(9, 2, 3, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ"),
        )
        val repository = FakeQuranReaderRepository(surah = surah, ayahs = ayahs)
        val viewModel = QuranReaderViewModel(repository, surahNumber = 2)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuranReaderUiState.Ready)
        state as QuranReaderUiState.Ready
        assertEquals(2, state.surah.surahNumber)
        assertEquals(listOf(1, 2, 3), state.ayahs.map { it.ayahNumberInSurah })
        assertEquals(exactText, state.ayahs.first().textUthmani)
        assertEquals(2, repository.requestedSurahNumber)
        assertEquals(2, repository.requestedAyahsSurahNumber)
    }

    @Test
    fun missingSurah_isTypedError() = runTest(mainDispatcher) {
        val viewModel = QuranReaderViewModel(
            FakeQuranReaderRepository(surah = null, ayahs = emptyList()),
            surahNumber = 2,
        )

        advanceUntilIdle()

        assertEquals(
            QuranReaderUiState.Error(QuranReaderError.SurahNotFound),
            viewModel.uiState.value,
        )
    }

    @Test
    fun noAyahs_isExplicitEmptyState() = runTest(mainDispatcher) {
        val viewModel = QuranReaderViewModel(
            FakeQuranReaderRepository(
                surah = testReaderSurah(2),
                ayahs = emptyList(),
            ),
            surahNumber = 2,
        )

        advanceUntilIdle()

        assertEquals(QuranReaderUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun invalidSurahNumber_isTypedErrorWithoutRepositoryRead() = runTest(mainDispatcher) {
        val repository = FakeQuranReaderRepository(
            surah = testReaderSurah(1),
            ayahs = listOf(testAyah(1, 1, 1, "text")),
        )
        val viewModel = QuranReaderViewModel(repository, surahNumber = 0)

        advanceUntilIdle()

        assertEquals(
            QuranReaderUiState.Error(QuranReaderError.InvalidSurahNumber),
            viewModel.uiState.value,
        )
        assertEquals(null, repository.requestedSurahNumber)
    }

    @Test
    fun repositoryFailure_isTypedError() = runTest(mainDispatcher) {
        val viewModel = QuranReaderViewModel(
            FakeQuranReaderRepository(
                surah = null,
                ayahs = emptyList(),
                failure = IllegalStateException("offline"),
            ),
            surahNumber = 2,
        )

        advanceUntilIdle()

        assertEquals(
            QuranReaderUiState.Error(QuranReaderError.LoadFailed),
            viewModel.uiState.value,
        )
    }

    private class FakeQuranReaderRepository(
        private val surah: Surah?,
        private val ayahs: List<Ayah>,
        private val failure: Throwable? = null,
    ) : QuranRepository {

        var requestedSurahNumber: Int? = null
        var requestedAyahsSurahNumber: Int? = null

        override fun observeSurahs() = kotlinx.coroutines.flow.flowOf(emptyList<Surah>())

        override suspend fun getSurah(surahNumber: Int): Surah? {
            requestedSurahNumber = surahNumber
            failure?.let { throw it }
            return surah
        }

        override suspend fun getAyahs(surahNumber: Int): List<Ayah> {
            requestedAyahsSurahNumber = surahNumber
            failure?.let { throw it }
            return ayahs
        }

        override suspend fun getAyah(
            surahNumber: Int,
            ayahNumberInSurah: Int,
        ): Ayah? = null

        override suspend fun getAyahRange(
            firstGlobal: Int,
            lastGlobal: Int,
        ): List<Ayah> = emptyList()

        override suspend fun search(query: String, limit: Int): List<Ayah> = emptyList()
    }
}

private fun testReaderSurah(number: Int): Surah = Surah(
    surahNumber = number,
    nameArabic = "البقرة",
    nameTransliteration = "Al-Baqarah",
    nameEnglish = "The Cow",
    ayahCount = 286,
    revelationOrder = 87,
    revelationType = RevelationType.MEDINAN,
    startPage = 2,
    basmala = "بِسْمِ",
)

private fun testAyah(
    globalNumber: Int,
    surahNumber: Int,
    ayahNumber: Int,
    text: String,
): Ayah = Ayah(
    globalAyahNumber = globalNumber,
    surahNumber = surahNumber,
    ayahNumberInSurah = ayahNumber,
    juzNumber = 1,
    pageNumber = 2,
    hasSajdah = false,
    textUthmani = text,
)
