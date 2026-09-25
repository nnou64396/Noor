package com.noor.islamic.data.local.userdata

import com.noor.islamic.data.local.athkar.AthkarCorpusLoader
import com.noor.islamic.data.local.db.practice.AthkarDhikrProgressEntity
import com.noor.islamic.data.local.db.practice.AthkarProgressDao
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.athkar.DhikrCount
import com.noor.islamic.domain.model.athkar.DhikrProgress
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.AthkarProgressSnapshot
import com.noor.islamic.domain.repository.AthkarProvenance
import com.noor.islamic.domain.repository.DhikrRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Read-only access to the bundled adhkar corpus plus the user's repetition
 * counters.
 *
 * Content and state are deliberately split: the corpus is immutable data from the
 * APK, while progress is mutable and stored by stable dhikr id. The UI can
 * therefore show a category that has no bundled content instead of crashing, and
 * progress rows for content that is no longer bundled are simply ignored.
 */
class AthkarPracticeRepository(
    private val corpusSource: AthkarCorpusLoader,
    private val progressDao: AthkarProgressDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DhikrRepository, AthkarProgressRepository {

    override suspend fun categories(): List<DhikrCategory> = corpusSource.load().categories

    override suspend fun dhikrIn(categoryId: String): List<Dhikr> =
        corpusSource.load().dhikrIn(categoryId)

    override suspend fun allDhikr(): List<Dhikr> = corpusSource.load().dhikr

    override suspend fun provenanceSummary(): AthkarProvenance = corpusSource.load().provenance

    override fun observeProgress(): Flow<AthkarProgressSnapshot> =
        progressDao.observeAll().map { rows ->
            AthkarProgressSnapshot(
                repetitionsByDhikrId = rows.associate { it.dhikrId to it.completedRepetitions },
            )
        }

    override suspend fun record(
        dhikrId: String,
        completedRepetitions: Int,
        count: DhikrCount,
    ) = withContext(ioDispatcher) {
        // Only a prescribed count may clamp a stored value. An unspecified count
        // deliberately has no ceiling and no fallback target, so the user's real
        // total is kept exactly as counted.
        val clamped = DhikrProgress.clampStored(count, completedRepetitions)
        val categoryId = corpusSource.load()
            .dhikr
            .firstOrNull { it.id == dhikrId }
            ?.categoryId
            .orEmpty()
        progressDao.upsert(
            AthkarDhikrProgressEntity(
                dhikrId = dhikrId,
                categoryId = categoryId,
                completedRepetitions = clamped,
                updatedAt = clock(),
            ),
        )
    }

    override suspend fun resetDhikr(dhikrId: String) = withContext(ioDispatcher) {
        progressDao.deleteByDhikrId(dhikrId)
    }

    override suspend fun resetCategory(categoryId: String, dhikrIds: List<String>) =
        withContext(ioDispatcher) {
            // Deleting by category is enough, but removing the exact ids keeps
            // rows consistent if a corpus revision re-categorised a dhikr.
            progressDao.deleteByCategory(categoryId)
            dhikrIds.forEach { progressDao.deleteByDhikrId(it) }
        }

    override suspend fun resetAll() = withContext(ioDispatcher) {
        progressDao.deleteAll()
    }
}
