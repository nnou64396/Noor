package com.noor.islamic.presentation.quran

import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.RevelationType
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class QuranSearchViewModelTest {

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
    fun initialState_isPromptAndDoesNotQueryTheIndex() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository()

        val viewModel = QuranSearchViewModel(repository)
        advanceUntilIdle()

        assertEquals(QuranSearchUiState.Prompt, viewModel.uiState.value)
        assertEquals(emptyList<String>(), repository.queries)
    }

    @Test
    fun typing_producesResultsForTheTrimmedQuery() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository(results = listOf(searchAyah(297, 2, 255)))

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("  ar-Rahman  ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuranSearchUiState.Results)
        state as QuranSearchUiState.Results
        assertEquals("ar-Rahman", state.query)
        assertEquals(listOf(297), state.results.map { it.globalAyahNumber })
        assertEquals(listOf("ar-Rahman"), repository.queries)
    }

    @Test
    fun rapidTyping_isDebouncedIntoASingleIndexQuery() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository(results = listOf(searchAyah(2, 2, 2)))

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("ا")
        advanceTimeBy(50)
        viewModel.onQueryChange("الرح")
        advanceTimeBy(50)
        viewModel.onQueryChange("الرحمن")
        advanceUntilIdle()

        assertEquals(listOf("الرحمن"), repository.queries)
    }

    @Test
    fun queryWithNoMatches_isAnExplicitEmptyResultState() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository(results = emptyList())

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("zzzz")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuranSearchUiState.Results)
        assertEquals(emptyList<Ayah>(), (state as QuranSearchUiState.Results).results)
    }

    @Test
    fun indexFailure_isReportedSeparatelyFromEmptyResults() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository(failure = IllegalStateException("db closed"))

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("الرحمن")
        advanceUntilIdle()

        assertEquals(
            QuranSearchUiState.Failed("الرحمن"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun clearing_returnsToPromptWithoutQuerying() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository()

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("الرحمن")
        advanceUntilIdle()
        viewModel.clear()
        advanceUntilIdle()

        assertEquals(QuranSearchUiState.Prompt, viewModel.uiState.value)
        assertEquals("", viewModel.query.value)
        assertEquals(listOf("الرحمن"), repository.queries)
    }

    @Test
    fun retry_rerunsTheSameQueryDespiteDistinctUntilChanged() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository()

        val viewModel = QuranSearchViewModel(repository)
        viewModel.onQueryChange("نور")
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf("نور", "نور"), repository.queries)
    }

    @Test
    fun surahMetadata_isLoadedOnceForResultLabels() = runTest(mainDispatcher) {
        val repository = RecordingSearchRepository()

        val viewModel = QuranSearchViewModel(repository)
        advanceUntilIdle()

        assertEquals(2, viewModel.surahs.value[2]?.surahNumber)
        assertEquals("Al-Baqarah", viewModel.surahs.value[2]?.nameTransliteration)
    }
}

private fun searchSurah(number: Int): Surah = Surah(
    surahNumber = number,
    nameArabic = "البقرة",
    nameTransliteration = "Al-Baqarah",
    nameEnglish = "The Cow",
    ayahCount = 286,
    revelationOrder = 87,
    revelationType = RevelationType.MEDINAN,
    startPage = 2,
    basmala = null,
)

private fun searchAyah(global: Int, surah: Int, ayah: Int): Ayah = Ayah(
    globalAyahNumber = global,
    surahNumber = surah,
    ayahNumberInSurah = ayah,
    juzNumber = 3,
    pageNumber = 42,
    hasSajdah = false,
    textUthmani = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
)

/** Records every query handed to the offline index, so the debounce can be asserted. */
private class RecordingSearchRepository(
    private val results: List<Ayah> = emptyList(),
    private val failure: Throwable? = null,
) : QuranRepository {

    val queries = mutableListOf<String>()
    private val surahs = MutableStateFlow(listOf(searchSurah(1), searchSurah(2)))

    override fun observeSurahs() = surahs

    override suspend fun getSurah(surahNumber: Int): Surah? = surahs.value
        .firstOrNull { it.surahNumber == surahNumber }

    override suspend fun getAyahs(surahNumber: Int): List<Ayah> = emptyList()

    override suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): Ayah? = null

    override suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<Ayah> = emptyList()

    override suspend fun search(query: String, limit: Int): List<Ayah> {
        queries += query
        failure?.let { throw it }
        return results.take(limit)
    }
}
