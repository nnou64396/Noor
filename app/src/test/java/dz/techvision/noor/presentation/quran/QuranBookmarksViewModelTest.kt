package dz.techvision.noor.presentation.quran

import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.Bookmark
import dz.techvision.noor.domain.model.quran.RevelationType
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.domain.repository.QuranBookmarksRepository
import dz.techvision.noor.domain.repository.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranBookmarksViewModelTest {

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
    fun noBookmarks_isAnExplicitEmptyState() = runTest(mainDispatcher) {
        val viewModel = QuranBookmarksViewModel(FakeBookmarkStore(), FakeSurahRepository())

        advanceUntilIdle()

        assertEquals(QuranBookmarksUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun bookmarks_areListedNewestFirstAndJoinedWithSurahNames() = runTest(mainDispatcher) {
        val store = FakeBookmarkStore()
        store.rows.value = listOf(
            Bookmark(AyahRef(1, 1, 1), createdAt = 100L),
            Bookmark(AyahRef(2, 255, 297), createdAt = 300L),
            Bookmark(AyahRef(1, 2, 2), createdAt = 200L),
        )
        val viewModel = QuranBookmarksViewModel(store, FakeSurahRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuranBookmarksUiState.Ready)
        val items = (state as QuranBookmarksUiState.Ready).items
        assertEquals(listOf(297, 2, 1), items.map { it.bookmark.ref.globalAyahNumber })
        assertEquals("Al-Baqarah", items.first().surah?.nameTransliteration)
    }

    @Test
    fun removingABookmark_dropsItFromTheList() = runTest(mainDispatcher) {
        val store = FakeBookmarkStore()
        store.rows.value = listOf(Bookmark(AyahRef(1, 1, 1), createdAt = 100L))
        val viewModel = QuranBookmarksViewModel(store, FakeSurahRepository())
        advanceUntilIdle()

        viewModel.remove(Bookmark(AyahRef(1, 1, 1), createdAt = 100L))
        advanceUntilIdle()

        assertEquals(listOf(AyahRef(1, 1, 1)), store.removed)
        assertEquals(QuranBookmarksUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun repositoryFailure_isTypedAsAnErrorState() = runTest(mainDispatcher) {
        val store = FakeBookmarkStore(failure = IllegalStateException("db closed"))
        val viewModel = QuranBookmarksViewModel(store, FakeSurahRepository())

        advanceUntilIdle()

        assertEquals(QuranBookmarksUiState.Error, viewModel.uiState.value)
    }
}

private class FakeBookmarkStore(
    private val failure: Throwable? = null,
) : QuranBookmarksRepository {

    val rows = MutableStateFlow<List<Bookmark>>(emptyList())
    val removed = mutableListOf<AyahRef>()

    override fun observeBookmarks(): Flow<List<Bookmark>> =
        if (failure == null) rows else flow { throw failure!! }

    override fun observeBookmarkedAyahNumbers(surahNumber: Int): Flow<Set<Int>> = flowOf(emptySet())

    override suspend fun add(ref: AyahRef): Boolean = true

    override suspend fun remove(ref: AyahRef) {
        removed += ref
        rows.value = rows.value.filterNot { it.ref == ref }
    }

    override suspend fun isBookmarked(ref: AyahRef): Boolean = false
}

private class FakeSurahRepository : QuranRepository {
    override fun observeSurahs(): Flow<List<Surah>> = flowOf(
        listOf(
            Surah(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, 1, RevelationType.MECCAN, 1, null),
            Surah(2, "البقرة", "Al-Baqarah", "The Cow", 286, 87, RevelationType.MEDINAN, 2, null),
        ),
    )

    override suspend fun getSurah(surahNumber: Int): Surah? = null
    override suspend fun getAyahs(surahNumber: Int): List<Ayah> = emptyList()
    override suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): Ayah? = null
    override suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<Ayah> = emptyList()
    override suspend fun search(query: String, limit: Int): List<Ayah> = emptyList()
}

