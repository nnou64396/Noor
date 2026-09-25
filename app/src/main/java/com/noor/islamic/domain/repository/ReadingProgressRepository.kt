package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.ReadingProgress
import kotlinx.coroutines.flow.Flow

/**
 * Local reading position, persisted on device only. No accounts, no cloud sync.
 */
interface ReadingProgressRepository {

    fun observeProgress(): Flow<ReadingProgress?>

    suspend fun saveProgress(ref: AyahRef)

    suspend fun clearProgress()
}
