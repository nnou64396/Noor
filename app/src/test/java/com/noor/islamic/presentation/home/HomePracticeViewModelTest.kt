package com.noor.islamic.presentation.home

import com.noor.islamic.data.local.athkar.AthkarCorpus
import com.noor.islamic.data.local.athkar.AthkarCorpusLoader
import com.noor.islamic.data.local.userdata.AthkarPracticeRepository
import com.noor.islamic.data.local.userdata.TasbihPracticeRepository
import com.noor.islamic.data.local.db.practice.AthkarDhikrProgressEntity
import com.noor.islamic.data.local.db.practice.AthkarProgressDao
import com.noor.islamic.data.local.db.practice.TasbihDao
import com.noor.islamic.data.local.db.practice.TasbihSessionEntity
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.domain.repository.AthkarProvenance
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The Home practice summary must report real progress and stay hidden when there
 * is nothing to show, so a card never appears with a half-loaded figure.
 */
class HomePracticeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun noContentAndNoCounting_showsNoCard() = runTest(dispatcher) {
        val state = awaitState(corpus = emptyCorpus, tasbih = TasbihSession.Empty)

        assertFalse(state.hasContent)
        assertEquals(0, state.tasbih.totalCount)
    }

    @Test
    fun withNoCorpusButCountedTasbih_theCardIsStillWorthShowing() = runTest(dispatcher) {
        val state = awaitState(
            corpus = emptyCorpus,
            tasbih = TasbihSession(count = 12, target = 33, totalCount = 12),
        )

        assertFalse(state.hasContent)
        assertEquals(12, state.tasbih.count)
        assertEquals(33, state.tasbih.target)
    }

    @Test
    fun completedAdhkar_areCountedAgainstTheirOwnTargets() = runTest(dispatcher) {
        val dao = AthkarProgressDaoFake()
        val tasbihDao = TasbihDaoFake()
        val viewModel = HomePracticeViewModel(
            dhikrRepository = AthkarPracticeRepository(
                corpusSource = CorpusLoaderFake(corpus()),
                progressDao = dao,
                ioDispatcher = Dispatchers.Unconfined,
            ),
            progressRepository = AthkarPracticeRepository(
                corpusSource = CorpusLoaderFake(corpus()),
                progressDao = dao,
                ioDispatcher = Dispatchers.Unconfined,
            ),
            tasbihRepository = TasbihPracticeRepository(
                tasbihDao = tasbihDao,
                ioDispatcher = Dispatchers.Unconfined,
            ),
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        // morning.1 (target 3) is done; morning.2 (target 1) is not.
        dao.rows.value = listOf(
            AthkarDhikrProgressEntity("morning.1", "morning", 3, 0L),
        )
        tasbihDao.row.value = TasbihSessionEntity(
            count = 5,
            target = 33,
            totalCount = 5,
            updatedAt = 0L,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value!!
        assertEquals(2, state.totalDhikr)
        assertEquals(1, state.completedDhikr)
        assertTrue(state.hasContent)
        assertEquals(5, state.tasbih.count)
    }

    @Test
    fun aFailingSource_leavesTheCardHidden() = runTest(dispatcher) {
        val viewModel = HomePracticeViewModel(
            dhikrRepository = object : com.noor.islamic.domain.repository.DhikrRepository {
                override suspend fun categories(): List<DhikrCategory> = error("unavailable")
                override suspend fun dhikrIn(categoryId: String): List<Dhikr> = error("unavailable")
                override suspend fun allDhikr(): List<Dhikr> = error("unavailable")
                override suspend fun provenanceSummary(): AthkarProvenance = error("unavailable")
            },
            progressRepository = AthkarPracticeRepository(
                corpusSource = CorpusLoaderFake(corpus()),
                progressDao = AthkarProgressDaoFake(),
                ioDispatcher = Dispatchers.Unconfined,
            ),
            tasbihRepository = TasbihPracticeRepository(
                tasbihDao = TasbihDaoFake(),
                ioDispatcher = Dispatchers.Unconfined,
            ),
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.awaitState(
        corpus: AthkarCorpus,
        tasbih: TasbihSession,
    ): HomePracticeUiState {
        val tasbihDao = TasbihDaoFake()
        if (tasbih.totalCount > 0) {
            tasbihDao.row.value = TasbihSessionEntity(
                count = tasbih.count,
                target = tasbih.target,
                totalCount = tasbih.totalCount,
                updatedAt = 0L,
            )
        }
        val viewModel = HomePracticeViewModel(
            dhikrRepository = AthkarPracticeRepository(
                corpusSource = CorpusLoaderFake(corpus),
                progressDao = AthkarProgressDaoFake(),
                ioDispatcher = Dispatchers.Unconfined,
            ),
            progressRepository = AthkarPracticeRepository(
                corpusSource = CorpusLoaderFake(corpus),
                progressDao = AthkarProgressDaoFake(),
                ioDispatcher = Dispatchers.Unconfined,
            ),
            tasbihRepository = TasbihPracticeRepository(
                tasbihDao = tasbihDao,
                ioDispatcher = Dispatchers.Unconfined,
            ),
        )
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel.uiState.value!!
    }

    private val emptyCorpus = AthkarCorpus.empty(note = "nothing bundled")

    private fun corpus() = AthkarCorpus(
        schemaVersion = 1,
        provenance = AthkarProvenance(
            corpusId = "test.corpus",
            title = "Test",
            isContentBundled = true,
            bundledDhikrCount = 2,
        ),
        categories = listOf(DhikrCategory("morning", 0, "Morning adhkar")),
        dhikr = listOf(
            dhikr("morning.1", 0, repeatCount = 3),
            dhikr("morning.2", 1, repeatCount = 1),
        ),
    )

    private fun dhikr(id: String, order: Int, repeatCount: Int) = Dhikr(
        id = id,
        categoryId = "morning",
        arabicText = "سُبْحَانَ اللَّهِ",
        translation = null,
        repeatCount = repeatCount,
        reference = "Reference",
        order = order,
    )

    private class CorpusLoaderFake(private val corpus: AthkarCorpus) : AthkarCorpusLoader {
        override suspend fun load(): AthkarCorpus = corpus
    }

    private class AthkarProgressDaoFake : AthkarProgressDao {
        val rows = MutableStateFlow<List<AthkarDhikrProgressEntity>>(emptyList())

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

    private class TasbihDaoFake : TasbihDao {
        val row = MutableStateFlow<TasbihSessionEntity?>(null)

        override fun observe(id: Int): Flow<TasbihSessionEntity?> = row

        override suspend fun upsert(entity: TasbihSessionEntity) {
            row.value = entity
        }

        override suspend fun deleteAll() {
            row.value = null
        }
    }
}
