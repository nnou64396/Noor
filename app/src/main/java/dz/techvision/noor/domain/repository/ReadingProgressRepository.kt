package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.ReadingProgress
import kotlinx.coroutines.flow.Flow

/**
 * Local reading position, persisted on device only. No accounts, no cloud sync.
 */
interface ReadingProgressRepository {

    fun observeProgress(): Flow<ReadingProgress?>

    suspend fun saveProgress(ref: AyahRef)

    suspend fun clearProgress()
}

