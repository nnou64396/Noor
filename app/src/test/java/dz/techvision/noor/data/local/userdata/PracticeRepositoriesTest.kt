package dz.techvision.noor.data.local.userdata

import dz.techvision.noor.data.local.athkar.AthkarCorpus
import dz.techvision.noor.data.local.athkar.AthkarCorpusLoader
import dz.techvision.noor.data.local.db.practice.AthkarDhikrProgressEntity
import dz.techvision.noor.data.local.db.practice.AthkarProgressDao
import dz.techvision.noor.data.local.db.practice.PracticeStateEntityMapper
import dz.techvision.noor.data.local.db.practice.TasbihDao
import dz.techvision.noor.data.local.db.practice.TasbihSessionEntity
import dz.techvision.noor.domain.model.athkar.Dhikr
import dz.techvision.noor.domain.model.athkar.DhikrCategory
import dz.techvision.noor.domain.model.athkar.DhikrCount
import dz.techvision.noor.domain.model.tasbih.TasbihSession
import dz.techvision.noor.domain.repository.AthkarProvenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room DAO behaviour is emulated in memory, so the repositories' mapping, clamping
 * and reset semantics are covered without an Android runtime.
 */
class PracticeRepositoriesTest {

    // ---- Athkar ------------------------------------------------------------

    @Test
    fun record_keepsOnlyCompletedRepetitionsPerStableId() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)

        repository.record("morning.a", completedRepetitions = 2, count = DhikrCount.Prescribed(3))
        repository.record("morning.b", completedRepetitions = 1, count = DhikrCount.Prescribed(3))
        repository.record("morning.a", completedRepetitions = 3, count = DhikrCount.Prescribed(3))

        val progress = repository.observeProgress().first()
        assertEquals(3, progress.repetitionsOf("morning.a"))
        assertEquals(1, progress.repetitionsOf("morning.b"))
        assertEquals(0, progress.repetitionsOf("morning.unknown"))
    }

    @Test
    fun record_clampsProgressToThePrescribedTarget() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)

        repository.record("morning.a", completedRepetitions = 99, count = DhikrCount.Prescribed(3))
        assertEquals(3, repository.observeProgress().first().repetitionsOf("morning.a"))

        repository.record("morning.a", completedRepetitions = -5, count = DhikrCount.Prescribed(3))
        assertEquals(0, repository.observeProgress().first().repetitionsOf("morning.a"))
    }

    @Test
    fun record_appliesNoCeilingWhenNoCountIsPrescribed() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)

        // An unspecified count must never be clamped to an invented target, and must
        // never be silently stored as 1 the way `target.coerceAtLeast(1)` did.
        listOf(1, 2, 3, 7, 33, 100, 5_000).forEach { counted ->
            repository.record("morning.a", completedRepetitions = counted, count = DhikrCount.Unspecified)
            assertEquals(counted, repository.observeProgress().first().repetitionsOf("morning.a"))
        }
    }

    @Test
    fun record_neverSubstitutesACommonCountForAnUnspecifiedOne() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)

        repository.record("morning.a", completedRepetitions = 40, count = DhikrCount.Unspecified)

        val stored = repository.observeProgress().first().repetitionsOf("morning.a")
        listOf(1, 3, 7, 33, 100).forEach { invented ->
            assertNotEquals("Unspecified must not be stored as $invented", invented, stored)
        }
        assertEquals(40, stored)
    }

    @Test
    fun record_storesTheCategorySoACategoryResetWorks() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)

        repository.record("morning.a", completedRepetitions = 1, count = DhikrCount.Prescribed(3))
        repository.record("evening.c", completedRepetitions = 1, count = DhikrCount.Prescribed(3))

        assertEquals("morning", dao.rows.value.first { it.dhikrId == "morning.a" }.categoryId)
        assertEquals("evening", dao.rows.value.first { it.dhikrId == "evening.c" }.categoryId)
    }

    @Test
    fun resetDhikr_clearsOnlyThatDhikr() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)
        repository.record("morning.a", completedRepetitions = 1, count = DhikrCount.Prescribed(3))
        repository.record("morning.b", completedRepetitions = 1, count = DhikrCount.Prescribed(3))

        repository.resetDhikr("morning.a")

        val progress = repository.observeProgress().first()
        assertEquals(0, progress.repetitionsOf("morning.a"))
        assertEquals(1, progress.repetitionsOf("morning.b"))
    }

    @Test
    fun resetCategory_clearsTheCategoryAndTheGivenIds() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)
        repository.record("morning.a", completedRepetitions = 1, count = DhikrCount.Prescribed(3))
        repository.record("morning.b", completedRepetitions = 1, count = DhikrCount.Prescribed(3))
        repository.record("evening.c", completedRepetitions = 1, count = DhikrCount.Prescribed(3))

        repository.resetCategory("morning", listOf("morning.a", "morning.b"))

        val progress = repository.observeProgress().first()
        assertEquals(0, progress.repetitionsOf("morning.a"))
        assertEquals(0, progress.repetitionsOf("morning.b"))
        assertEquals(1, progress.repetitionsOf("evening.c"))
    }

    @Test
    fun resetAll_clearsEverything() = runTest {
        val dao = FakeAthkarProgressDao()
        val repository = athkarRepository(dao)
        repository.record("morning.a", completedRepetitions = 1, count = DhikrCount.Prescribed(3))

        repository.resetAll()

        assertEquals(emptyMap<String, Int>(), repository.observeProgress().first().repetitionsByDhikrId)
    }

    @Test
    fun corpusReads_areServedFromTheLoader() = runTest {
        val loader = FakeCorpusLoader(athkarCorpus())
        val repository = AthkarPracticeRepository(
            corpusSource = loader,
            progressDao = FakeAthkarProgressDao(),
        )

        assertEquals(2, repository.categories().size)
        assertEquals(listOf("morning.a", "morning.b"), repository.dhikrIn("morning").map { it.id })
        assertEquals(3, repository.allDhikr().size)
        assertEquals("test.corpus", repository.provenanceSummary().corpusId)
    }

    @Test
    fun anEmptyCorpus_yieldsNoCategoriesAndNoDhikr() = runTest {
        val repository = AthkarPracticeRepository(
            corpusSource = FakeCorpusLoader(AthkarCorpus.empty(note = "nothing bundled")),
            progressDao = FakeAthkarProgressDao(),
        )

        assertEquals(emptyList<DhikrCategory>(), repository.categories())
        assertEquals(emptyList<Dhikr>(), repository.allDhikr())
        assertEquals(emptyList<Dhikr>(), repository.dhikrIn("morning"))
        assertFalse(repository.provenanceSummary().isContentBundled)
    }

    // ---- Tasbih ------------------------------------------------------------

    @Test
    fun tasbihSession_startsEmptyBeforeAnythingIsSaved() = runTest {
        val repository = TasbihPracticeRepository(FakeTasbihDao())

        assertEquals(TasbihSession.Empty, repository.observeSession().first())
    }

    @Test
    fun tasbihSession_roundTripsThroughTheEntity() = runTest {
        val dao = FakeTasbihDao()
        val repository = TasbihPracticeRepository(
            tasbihDao = dao,
            clock = { 1_700_000_000_000L },
        )
        val session = TasbihSession(count = 7, target = 33, totalCount = 12)

        repository.save(session)

        assertEquals(session, repository.observeSession().first())
        assertEquals(1_700_000_000_000L, dao.row.value?.updatedAt)
    }

    @Test
    fun tasbihSession_sanitizesCorruptedStoredValues() = runTest {
        val dao = FakeTasbihDao()
        dao.row.value = TasbihSessionEntity(
            count = 900,
            target = -3,
            totalCount = -1,
            updatedAt = 0L,
        )

        val restored = TasbihPracticeRepository(dao).observeSession().first()

        // Target is clamped into range, then the count into 0..target.
        assertEquals(TasbihSession.MIN_TARGET, restored.target)
        assertEquals(TasbihSession.MIN_TARGET, restored.count)
        assertEquals(0, restored.totalCount)
    }

    @Test
    fun tasbihEntity_neverWritesOutOfRangeValues() {
        val entity = PracticeStateEntityMapper.toEntity(
            TasbihSession(count = 500, target = 33, totalCount = -2),
            updatedAt = 42L,
        )

        assertEquals(33, entity.count)
        assertEquals(0, entity.totalCount)
        assertEquals(42L, entity.updatedAt)
    }

    // ---- fakes -------------------------------------------------------------

    private fun athkarRepository(dao: AthkarProgressDao) = AthkarPracticeRepository(
        corpusSource = FakeCorpusLoader(athkarCorpus()),
        progressDao = dao,
        clock = { 1_000L },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun athkarCorpus() = AthkarCorpus(
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
            dhikr("morning.a", "morning", 0, 3),
            dhikr("morning.b", "morning", 1, 1),
            dhikr("evening.c", "evening", 0, 3),
        ),
    )

    private fun dhikr(id: String, categoryId: String, order: Int, repeat: Int) = Dhikr(
        id = id,
        categoryId = categoryId,
        arabicText = "سُبْحَانَ اللَّهِ",
        translation = null,
        repeatCount = DhikrCount.Prescribed(repeat),
        reference = "Reference",
        order = order,
    )

    private class FakeCorpusLoader(private val corpus: AthkarCorpus) : AthkarCorpusLoader {
        override suspend fun load(): AthkarCorpus = corpus
    }

    private class FakeAthkarProgressDao : AthkarProgressDao {
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

    private class FakeTasbihDao : TasbihDao {
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

