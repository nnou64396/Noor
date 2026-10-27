package dz.techvision.noor.presentation.quran

import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.RevelationType
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.domain.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelTest {

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
    fun successState_containsRepositorySurahs() = runTest(mainDispatcher) {
        val surahs = listOf(testSurah(1), testSurah(2))
        val viewModel = QuranViewModel(FakeQuranRepository(surahs = surahs))

        advanceUntilIdle()

        assertEquals(QuranUiState.Success(surahs), viewModel.uiState.value)
    }

    @Test
    fun emptyState_isUsedWhenRepositoryReturnsNoSurahs() = runTest(mainDispatcher) {
        val viewModel = QuranViewModel(FakeQuranRepository(surahs = emptyList()))

        advanceUntilIdle()

        assertEquals(QuranUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun errorState_isTypedWhenRepositoryFails() = runTest(mainDispatcher) {
        val viewModel = QuranViewModel(
            FakeQuranRepository(failure = IllegalStateException("offline")),
        )

        advanceUntilIdle()

        assertEquals(
            QuranUiState.Error(QuranIndexError.LoadFailed),
            viewModel.uiState.value,
        )
    }

    private class FakeQuranRepository(
        private val surahs: List<Surah> = emptyList(),
        private val failure: Throwable? = null,
    ) : QuranRepository {

        override fun observeSurahs(): Flow<List<Surah>> = if (failure == null) {
            flowOf(surahs)
        } else {
            flow { throw failure }
        }

        override suspend fun getSurah(surahNumber: Int): Surah? = null

        override suspend fun getAyahs(surahNumber: Int): List<Ayah> = emptyList()

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

private fun testSurah(number: Int): Surah = Surah(
    surahNumber = number,
    nameArabic = "سورة $number",
    nameTransliteration = "Surah $number",
    nameEnglish = "Surah $number",
    ayahCount = 7,
    revelationOrder = number,
    revelationType = RevelationType.MECCAN,
    startPage = 1,
    basmala = null,
)

