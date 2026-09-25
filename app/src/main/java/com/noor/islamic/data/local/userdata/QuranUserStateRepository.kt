package com.noor.islamic.data.local.userdata

import com.noor.islamic.data.local.db.userstate.QuranBookmarkDao
import com.noor.islamic.data.local.db.userstate.QuranReadingProgressDao
import com.noor.islamic.data.local.db.userstate.QuranUserStateEntityMapper
import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.Bookmark
import com.noor.islamic.domain.model.quran.ReadingProgress
import com.noor.islamic.domain.repository.QuranBookmarksRepository
import com.noor.islamic.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Single implementation of both Phase 4 user-state repositories, backed by the
 * app-private `quran_user_state.db` table. One instance is shared through the
 * service locator so no screen opens a second database handle.
 */
class QuranUserStateRepository(
    private val bookmarkDao: QuranBookmarkDao,
    private val readingProgressDao: QuranReadingProgressDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : QuranBookmarksRepository, ReadingProgressRepository {

    override fun observeBookmarks(): Flow<List<Bookmark>> =
        bookmarkDao.observeAll().map { rows -> rows.map(QuranUserStateEntityMapper::bookmarkToDomain) }

    override fun observeBookmarkedAyahNumbers(surahNumber: Int): Flow<Set<Int>> =
        bookmarkDao.observeAyahNumbersOfSurah(surahNumber).map { it.toSet() }

    override suspend fun add(ref: AyahRef): Boolean = withContext(ioDispatcher) {
        bookmarkDao.insert(QuranUserStateEntityMapper.bookmarkToEntity(ref, clock())) != -1L
    }

    override suspend fun remove(ref: AyahRef) = withContext(ioDispatcher) {
        bookmarkDao.deleteByGlobalNumber(ref.globalAyahNumber)
    }

    override suspend fun isBookmarked(ref: AyahRef): Boolean = withContext(ioDispatcher) {
        bookmarkDao.exists(ref.globalAyahNumber)
    }

    override fun observeProgress(): Flow<ReadingProgress?> =
        readingProgressDao.observe().map { it?.let(QuranUserStateEntityMapper::progressToDomain) }

    override suspend fun saveProgress(ref: AyahRef) = withContext(ioDispatcher) {
        readingProgressDao.upsert(QuranUserStateEntityMapper.progressToEntity(ref, clock()))
    }

    override suspend fun clearProgress() = withContext(ioDispatcher) {
        readingProgressDao.clear()
    }
}
