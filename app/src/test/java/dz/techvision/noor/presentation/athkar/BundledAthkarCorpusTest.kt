package dz.techvision.noor.presentation.athkar

import dz.techvision.noor.data.local.athkar.AssetAthkarCorpusSource
import dz.techvision.noor.data.local.db.practice.AthkarDhikrProgressEntity
import dz.techvision.noor.data.local.db.practice.AthkarProgressDao
import dz.techvision.noor.data.local.userdata.AthkarPracticeRepository
import dz.techvision.noor.domain.model.athkar.DhikrCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Exercises the production asset, repository, and ViewModel together.
 *
 * The parser unit tests only prove the unminified code can read the bundle.
 * These tests prove the shipped content reaches the repository and produces
 * [AthkarUiState.Ready] instead of the empty "coming soon" state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BundledAthkarCorpusTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun repository_servesTheRealBundledCategoriesAndArabicDhikr() = runTest(dispatcher) {
        val repository = repository()

        val categories = repository.categories()
        val dhikr = repository.allDhikr()
        val provenance = repository.provenanceSummary()

        assertEquals(132, categories.size)
        assertEquals(267, dhikr.size)
        assertEquals(dhikr.size, provenance.bundledDhikrCount)
        assertEquals("rn0x/Adhkar-json", provenance.corpusId)
        assertTrue(provenance.isContentBundled)

        assertTrue(categories.all { it.titleResKey.isNotBlank() })
        assertEquals(24, repository.dhikrIn(categories.first().id).size)

        val firstDhikr = repository.dhikrIn(categories.first().id).first()
        assertTrue(firstDhikr.arabicText.any { it in '\u0600'..'\u06FF' })
        val count = firstDhikr.repeatCount
        assertTrue(count is DhikrCount.Prescribed && count.value > 0)
    }

    @Test
    fun viewModel_reachesReadyForTheRealBundledCorpus() = runTest(dispatcher) {
        val repository = repository()
        val viewModel = AthkarViewModel(repository, repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Ready but was $state", state is AthkarUiState.Ready)
        val ready = state as AthkarUiState.Ready

        assertEquals(132, ready.items.size)
        assertEquals(267, ready.items.sumOf { it.progress.totalDhikr })
        assertEquals(267, ready.provenance.bundledDhikrCount)
        assertEquals("source-1", ready.items.first().category.id)
        assertTrue(ready.items.all { it.progress.totalDhikr > 0 })
    }

    private fun repository(): AthkarPracticeRepository = AthkarPracticeRepository(
        corpusSource = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
        ),
        progressDao = FakeAthkarProgressDao(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private class FakeAthkarProgressDao : AthkarProgressDao {
        private val rows = MutableStateFlow<List<AthkarDhikrProgressEntity>>(emptyList())

        override fun observeAll(): Flow<List<AthkarDhikrProgressEntity>> = rows

        override suspend fun upsert(entity: AthkarDhikrProgressEntity) {
            rows.value = rows.value.filterNot { it.dhikrId == entity.dhikrId } + entity
        }

        override suspend fun deleteByDhikrId(dhikrId: String) {
            rows.value = rows.value.filterNot { it.dhikrId == dhikrId }
        }

        override suspend fun deleteByCategory(categoryId: String) {
            rows.value = rows.value.filterNot { it.categoryId == categoryId }
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }
    }
}

