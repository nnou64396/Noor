package com.noor.islamic.presentation.quran

import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.Bookmark
import com.noor.islamic.domain.model.quran.RevelationType
import com.noor.islamic.domain.model.quran.ReadingProgress
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranBookmarksRepository
import com.noor.islamic.domain.repository.QuranRepository
import com.noor.islamic.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Phase 4 reader behaviour: saved-position restore, bookmarking and progress writes. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranReaderPhase4ViewModelTest {

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
    fun storedProgress_opensTheReaderAtTheSavedAyah() = runTest(mainDispatcher) {
        val bookmarks = FakeBookmarks()
        val progress = FakeProgress()
        progress.store(ref(ayahNumber = 3))
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            bookmarks = bookmarks,
            readingProgress = progress,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuranReaderUiState.Ready)
        assertEquals(2, (state as QuranReaderUiState.Ready).currentIndex)
    }

    @Test
    fun explicitAyahArgument_winsOverStoredProgress() = runTest(mainDispatcher) {
        val progress = FakeProgress()
        progress.store(ref(ayahNumber = 3))
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            initialAyahNumber = 2,
            readingProgress = progress,
        )

        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value as QuranReaderUiState.Ready).currentIndex)
    }

    @Test
    fun bookmarksFromAnotherSurah_doNotMarkAyahsOfThisSurah() = runTest(mainDispatcher) {
        val bookmarks = FakeBookmarks()
        bookmarks.rows.value = listOf(Bookmark(AyahRef(2, 255, 297), createdAt = 1L))
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            bookmarks = bookmarks,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as QuranReaderUiState.Ready
        assertEquals(emptySet<Int>(), state.bookmarkedAyahNumbers)
    }

    @Test
    fun togglingBookmarkOfAnyAyah_savesThatAyahNotTheCurrentOne() = runTest(mainDispatcher) {
        val bookmarks = FakeBookmarks()
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            bookmarks = bookmarks,
        )
        advanceUntilIdle()

        viewModel.toggleBookmarkOf(phase4Ayahs()[2])
        advanceUntilIdle()

        assertEquals(listOf(6_233), bookmarks.added.map { it.globalAyahNumber })
    }

    @Test
    fun selectingAnAyah_persistsProgressAfterTheDebounce() = runTest(mainDispatcher) {
        val progress = FakeProgress()
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            readingProgress = progress,
        )
        advanceUntilIdle()

        viewModel.selectAyah(1)
        advanceUntilIdle()

        // index 1 is the second ayah of the surah.
        assertEquals(2, progress.saved.last().ayahNumberInSurah)
    }

    @Test
    fun flushProgress_writesTheCurrentAyahImmediatelyOnLeaving() = runTest(mainDispatcher) {
        val progress = FakeProgress()
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
            readingProgress = progress,
        )
        advanceUntilIdle()
        viewModel.selectAyah(2)
        progress.saved.clear()

        viewModel.flushProgress()
        advanceUntilIdle()

        assertEquals(3, progress.saved.last().ayahNumberInSurah)
    }

    @Test
    fun audioIsAbsent_withoutAControllerSoTheReaderStaysTextOnly() = runTest(mainDispatcher) {
        val viewModel = QuranReaderViewModel(
            repository = FakeReaderRepository(phase4Ayahs()),
            surahNumber = 112,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as QuranReaderUiState.Ready
        assertEquals(null, state.audio)
        // Playback requests are safe no-ops instead of crashing.
        viewModel.togglePlayback()
        viewModel.skipToNextAyah()
        viewModel.seekAudio(1_000L)
        viewModel.setRepeatCurrentSurah(true)
        viewModel.selectReciter(com.noor.islamic.data.audio.EveryAyahReciterCatalog.defaultReciter)
        assertFalse(viewModel.uiState.value is QuranReaderUiState.Error)
    }
}

private fun phase4Surah(): Surah = Surah(
    surahNumber = 112,
    nameArabic = "الإخلاص",
    nameTransliteration = "Al-Ikhlas",
    nameEnglish = "The Sincerity",
    ayahCount = 4,
    revelationOrder = 112,
    revelationType = RevelationType.MECCAN,
    startPage = 604,
    basmala = null,
)

private fun phase4Ayahs(): List<Ayah> = listOf(
    phase4Ayah(1),
    phase4Ayah(2),
    phase4Ayah(3),
    phase4Ayah(4),
)

private fun phase4Ayah(ayahNumber: Int): Ayah = Ayah(
    globalAyahNumber = 6_230 + ayahNumber,
    surahNumber = 112,
    ayahNumberInSurah = ayahNumber,
    juzNumber = 30,
    pageNumber = 604,
    hasSajdah = false,
    textUthmani = "آية $ayahNumber",
)

private fun ref(ayahNumber: Int): AyahRef = AyahRef(
    surahNumber = 112,
    ayahNumberInSurah = ayahNumber,
    globalAyahNumber = 6_230 + ayahNumber,
)

private class FakeProgress : ReadingProgressRepository {
    private val value = MutableStateFlow<ReadingProgress?>(null)
    val saved = mutableListOf<AyahRef>()

    fun store(ref: AyahRef) {
        value.value = ReadingProgress(ref, updatedAt = 0L)
    }

    override fun observeProgress(): Flow<ReadingProgress?> = value

    override suspend fun saveProgress(ref: AyahRef) {
        saved += ref
        value.value = ReadingProgress(ref, updatedAt = saved.size.toLong())
    }

    override suspend fun clearProgress() {
        value.value = null
    }
}

private class FakeBookmarks : QuranBookmarksRepository {
    val rows = MutableStateFlow<List<Bookmark>>(emptyList())
    val added = mutableListOf<AyahRef>()

    override fun observeBookmarks(): Flow<List<Bookmark>> = rows

    override fun observeBookmarkedAyahNumbers(surahNumber: Int): Flow<Set<Int>> =
        rows.map { list ->
            list.filter { it.ref.surahNumber == surahNumber }.map { it.ref.ayahNumberInSurah }.toSet()
        }

    override suspend fun add(ref: AyahRef): Boolean {
        added += ref
        rows.value = rows.value + Bookmark(ref, createdAt = added.size.toLong())
        return true
    }

    override suspend fun remove(ref: AyahRef) {
        rows.value = rows.value.filterNot { it.ref == ref }
    }

    override suspend fun isBookmarked(ref: AyahRef): Boolean =
        rows.value.any { it.ref == ref }
}

private class FakeReaderRepository(
    private val ayahs: List<Ayah>,
) : QuranRepository {

    override fun observeSurahs(): Flow<List<Surah>> = flowOf(listOf(phase4Surah()))

    override suspend fun getSurah(surahNumber: Int): Surah? = phase4Surah()

    override suspend fun getAyahs(surahNumber: Int): List<Ayah> = ayahs

    override suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): Ayah? =
        ayahs.firstOrNull { it.ayahNumberInSurah == ayahNumberInSurah }

    override suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<Ayah> = emptyList()

    override suspend fun search(query: String, limit: Int): List<Ayah> = emptyList()
}
