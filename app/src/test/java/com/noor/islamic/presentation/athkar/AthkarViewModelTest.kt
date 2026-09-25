package com.noor.islamic.presentation.athkar

import com.noor.islamic.data.local.athkar.AthkarCorpus
import com.noor.islamic.data.local.db.practice.AthkarDhikrProgressEntity
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.AthkarProgressSnapshot
import com.noor.islamic.domain.repository.AthkarProvenance
import com.noor.islamic.domain.repository.DhikrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

/**
 * Both Athkar ViewModels over an in-memory repository. The Arabic here is a
 * fixture only: it exercises counters, never displayed wording.
 */
class AthkarViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun categories_areListedInSourceOrderWithTheirCounts() = runTest(dispatcher) {
        val viewModel = AthkarViewModel(FakeAthkarRepository(corpus()), FakeAthkarRepository(corpus()))
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value as AthkarUiState.Ready
        assertEquals(listOf("morning", "evening"), state.items.map { it.category.id })
        assertEquals(listOf(2, 1), state.items.map { it.progress.totalDhikr })
        assertEquals(listOf(0, 0), state.items.map { it.progress.completedDhikr })
    }

    @Test
    fun categoryProgress_countsCompletedDhikr() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        // morning.1 needs 3 repetitions and has 3, so it counts as one done while
        // morning.2 (a single repetition) has not been counted yet.
        repository.rows.value = listOf(
            progress("morning.1", "morning", completed = 3),
            progress("morning.2", "morning", completed = 0),
        )
        advanceUntilIdle()

        val morning = (viewModel.uiState.value as AthkarUiState.Ready)
            .items.single { it.category.id == "morning" }
            .progress
        assertEquals(2, morning.totalDhikr)
        assertEquals(1, morning.completedDhikr)
        assertFalse(morning.isComplete)
        assertEquals(0.5f, morning.fraction, 0.0001f)
    }

    @Test
    fun aFinishedCategory_isReportedComplete() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(
            progress("morning.1", "morning", completed = 3),
            progress("morning.2", "morning", completed = 1),
        )
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val morning = (viewModel.uiState.value as AthkarUiState.Ready)
            .items.single { it.category.id == "morning" }
            .progress
        assertTrue(morning.isComplete)
    }

    @Test
    fun resetAll_clearsEveryRecordedRepetition() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(progress("morning.1", "morning", completed = 3))
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.resetAll()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AthkarUiState.Ready
        assertTrue(state.items.all { it.progress.completedDhikr == 0 })
    }

    @Test
    fun anEmptyCorpus_showsNoContentInsteadOfInventingCategories() = runTest(dispatcher) {
        val empty = AthkarCorpus.empty(note = "nothing bundled")
        val repository = FakeAthkarRepository(empty)
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value as AthkarUiState.NoContentBundled
        assertFalse(state.provenance.isContentBundled)
        assertEquals(0, state.provenance.bundledDhikrCount)
        assertEquals(AthkarCorpus.NONE_CORPUS_ID, state.provenance.corpusId)
    }

    @Test
    fun categoriesWithoutDhikr_areNotOffered() = runTest(dispatcher) {
        val withEmptyCategory = corpus().copy(
            categories = corpus().categories + DhikrCategory("empty_category", 2, "Empty"),
        )
        val repository = FakeAthkarRepository(withEmptyCategory)
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value as AthkarUiState.Ready
        assertEquals(listOf("morning", "evening"), state.items.map { it.category.id })
    }

    @Test
    fun aFailingLoader_showsTheErrorState() = runTest(dispatcher) {
        val failing = FailingAthkarRepository()
        val viewModel = AthkarViewModel(failing, failing)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AthkarUiState.Error)
    }

    @Test
    fun repeat_countsUpToTheRequiredRepetitions() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        repeat(3) { viewModel.repeat(dhikrOf(viewModel, "morning.1")) }
        advanceUntilIdle()

        val item = itemOf(viewModel, "morning.1")
        assertTrue(item.isComplete)
        assertEquals(0, item.remaining)
        assertEquals(1f, item.fraction, 0.0001f)
    }

    @Test
    fun rapidRepeats_areEachCounted() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        // morning.2 needs one repetition, so the extra taps must not overshoot.
        val dhikr = dhikrOf(viewModel, "morning.2")
        repeat(5) { viewModel.repeat(dhikr) }
        advanceUntilIdle()

        assertEquals(1, itemOf(viewModel, "morning.2").completedRepetitions)
        assertEquals(
            1,
            repository.rows.value.single { it.dhikrId == "morning.2" }.completedRepetitions,
        )
    }

    @Test
    fun repeat_afterCompletion_doesNotOvershoot() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(progress("morning.1", "morning", completed = 3))
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.repeat(dhikrOf(viewModel, "morning.1"))
        advanceUntilIdle()

        assertEquals(3, itemOf(viewModel, "morning.1").completedRepetitions)
    }

    @Test
    fun storedProgressAboveTheTarget_isClampedForDisplay() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(progress("morning.2", "morning", completed = 99))
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val item = itemOf(viewModel, "morning.2")
        assertEquals(1, item.completedRepetitions)
        assertTrue(item.isComplete)
    }

    @Test
    fun anUnknownSlug_isReportedAsNotFound() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        val viewModel = AthkarCategoryViewModel("nope", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AthkarCategoryUiState.NotFound)
    }

    @Test
    fun aCategoryWithNoDhikr_isReportedAsNotFound() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(
            corpus().copy(
                categories = corpus().categories + DhikrCategory("empty_category", 2, "Empty"),
            ),
        )
        val viewModel = AthkarCategoryViewModel("empty_category", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AthkarCategoryUiState.NotFound)
    }

    @Test
    fun resetCategory_clearsOnlyThisCategory() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(
            progress("morning.1", "morning", completed = 3),
            progress("evening.1", "evening", completed = 1),
        )
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.resetCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AthkarCategoryUiState.Ready
        assertEquals(0, state.completedCount)
        assertEquals("evening.1", repository.rows.value.single().dhikrId)
    }

    @Test
    fun resetDhikr_clearsOnlyThatDhikr() = runTest(dispatcher) {
        val repository = FakeAthkarRepository(corpus())
        repository.rows.value = listOf(
            progress("morning.1", "morning", completed = 3),
            progress("morning.2", "morning", completed = 1),
        )
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.resetDhikr(dhikrOf(viewModel, "morning.1"))
        advanceUntilIdle()

        assertEquals(setOf("morning.2"), repository.rows.value.map { it.dhikrId }.toSet())
    }

    @Test
    fun itemFraction_staysInRange() {
        val item = AthkarItemUi(
            dhikr("morning.1", "morning", 0, repeatCount = 4),
            completedRepetitions = 0,
        )

        assertEquals(0f, item.fraction, 0.0001f)
        assertEquals(0.5f, item.copy(completedRepetitions = 2).fraction, 0.0001f)
        assertEquals(1f, item.copy(completedRepetitions = 4).fraction, 0.0001f)
        assertEquals(0, item.copy(completedRepetitions = 4).remaining)
    }

    private fun dhikrOf(viewModel: AthkarCategoryViewModel, id: String): Dhikr =
        itemOf(viewModel, id).dhikr

    private fun itemOf(viewModel: AthkarCategoryViewModel, id: String): AthkarItemUi =
        (viewModel.uiState.value as AthkarCategoryUiState.Ready).items.single { it.dhikr.id == id }

    private fun corpus() = AthkarCorpus(
        schemaVersion = 1,
        provenance = AthkarProvenance(
            corpusId = "test.corpus",
            title = "Test",
            isContentBundled = true,
            bundledDhikrCount = 3,
        ),
        categories = listOf(
            DhikrCategory("morning", 0, "Morning adhkar"),
            DhikrCategory("evening", 1, "Evening adhkar"),
        ),
        dhikr = listOf(
            dhikr("morning.1", "morning", 0, repeatCount = 3),
            dhikr("morning.2", "morning", 1, repeatCount = 1),
            dhikr("evening.1", "evening", 0, repeatCount = 1),
        ),
    )

    private fun dhikr(id: String, categoryId: String, order: Int, repeatCount: Int) = Dhikr(
        id = id,
        categoryId = categoryId,
        arabicText = "سُبْحَانَ اللَّهِ",
        translation = null,
        repeatCount = repeatCount,
        reference = "Reference",
        order = order,
    )

    private fun progress(dhikrId: String, categoryId: String, completed: Int) =
        AthkarDhikrProgressEntity(
            dhikrId = dhikrId,
            categoryId = categoryId,
            completedRepetitions = completed,
            updatedAt = 0L,
        )

    /**
     * Serves a fixed corpus and keeps the repetition counters in memory, so the
     * ViewModels are exercised through their domain contracts alone.
     */
    private open class FakeAthkarRepository(
        private val corpus: AthkarCorpus,
    ) : DhikrRepository, AthkarProgressRepository {

        val rows = MutableStateFlow<List<AthkarDhikrProgressEntity>>(emptyList())

        override suspend fun categories(): List<DhikrCategory> = corpus.categories

        override suspend fun dhikrIn(categoryId: String): List<Dhikr> = corpus.dhikrIn(categoryId)

        override suspend fun allDhikr(): List<Dhikr> = corpus.dhikr

        override suspend fun provenanceSummary(): AthkarProvenance = corpus.provenance

        override fun observeProgress(): Flow<AthkarProgressSnapshot> =
            rows.map { entities ->
                AthkarProgressSnapshot(entities.associate { it.dhikrId to it.completedRepetitions })
            }

        override suspend fun record(dhikrId: String, completedRepetitions: Int, target: Int) {
            val clamped = completedRepetitions.coerceIn(0, target.coerceAtLeast(1))
            val categoryId = corpus.dhikr.firstOrNull { it.id == dhikrId }?.categoryId.orEmpty()
            rows.value = rows.value.filterNot { it.dhikrId == dhikrId } + AthkarDhikrProgressEntity(
                dhikrId = dhikrId,
                categoryId = categoryId,
                completedRepetitions = clamped,
                updatedAt = 0L,
            )
        }

        override suspend fun resetDhikr(dhikrId: String) {
            rows.value = rows.value.filterNot { it.dhikrId == dhikrId }
        }

        override suspend fun resetCategory(categoryId: String, dhikrIds: List<String>) {
            rows.value = rows.value.filterNot {
                it.categoryId == categoryId || it.dhikrId in dhikrIds
            }
        }

        override suspend fun resetAll() {
            rows.value = emptyList()
        }
    }

    private class FailingAthkarRepository : FakeAthkarRepository(AthkarCorpus.empty()) {
        override suspend fun categories(): List<DhikrCategory> = error("corpus unavailable")
    }
}
