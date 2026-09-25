package com.noor.islamic.data.local.userdata

import com.noor.islamic.data.local.db.userstate.QuranBookmarkDao
import com.noor.islamic.data.local.db.userstate.QuranBookmarkEntity
import com.noor.islamic.data.local.db.userstate.QuranReadingProgressDao
import com.noor.islamic.data.local.db.userstate.QuranReadingProgressEntity
import com.noor.islamic.domain.model.quran.AyahRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room DAO behaviour is emulated in memory: these tests cover the repository's mapping,
 * insert-ignore semantics and progress replacement without needing an Android runtime.
 */
class QuranUserStateRepositoryTest {

    private val baqarah255 = AyahRef(surahNumber = 2, ayahNumberInSurah = 255, globalAyahNumber = 297)
    private val baqarah256 = AyahRef(surahNumber = 2, ayahNumberInSurah = 256, globalAyahNumber = 298)
    private val fatiha1 = AyahRef(surahNumber = 1, ayahNumberInSurah = 1, globalAyahNumber = 1)

    @Test
    fun addAndRemove_keepsOnlyTheBookmarkedAyahs() = runTest {
        val bookmarkDao = FakeQuranBookmarkDao()
        val repository = repository(bookmarkDao)

        assertTrue(repository.add(baqarah255))
        assertTrue(repository.isBookmarked(baqarah255))
        assertEquals(listOf(297), repository.observeBookmarks().first().map { it.ref.globalAyahNumber })

        repository.remove(baqarah255)

        assertFalse(repository.isBookmarked(baqarah255))
        assertEquals(emptyList<Int>(), repository.observeBookmarks().first().map { it.ref.globalAyahNumber })
    }

    @Test
    fun add_isIdempotentAndReportsExistingBookmarks() = runTest {
        val bookmarkDao = FakeQuranBookmarkDao()
        val repository = repository(bookmarkDao)

        assertTrue(repository.add(baqarah255))
        // The unique index rejects the second insert, which the DAO reports as -1.
        assertFalse(repository.add(baqarah255))
        assertEquals(1, bookmarkDao.rows.value.size)
    }

    @Test
    fun observeBookmarkedAyahNumbers_isScopedToOneSurah() = runTest {
        val repository = repository(FakeQuranBookmarkDao())

        repository.add(baqarah255)
        repository.add(baqarah256)
        repository.add(fatiha1)

        assertEquals(setOf(255, 256), repository.observeBookmarkedAyahNumbers(2).first())
        assertEquals(setOf(1), repository.observeBookmarkedAyahNumbers(1).first())
        assertEquals(emptySet<Int>(), repository.observeBookmarkedAyahNumbers(3).first())
    }

    @Test
    fun observeBookmarks_preservesCanonicalRefsAndTimestamps() = runTest {
        val bookmarkDao = FakeQuranBookmarkDao()
        val repository = repository(bookmarkDao, clock = { bookmarkDao.clock })

        bookmarkDao.clock = 100L
        repository.add(baqarah255)
        bookmarkDao.clock = 200L
        repository.add(baqarah256)

        val stored = repository.observeBookmarks().first()
        assertEquals(listOf(baqarah255, baqarah256), stored.map { it.ref })
        assertEquals(listOf(100L, 200L), stored.map { it.createdAt })
    }

    @Test
    fun saveProgress_replacesTheSingleStoredPosition() = runTest {
        val progressDao = FakeQuranReadingProgressDao()
        val repository = repository(progressDao = progressDao)

        assertNull(repository.observeProgress().first())

        repository.saveProgress(fatiha1)
        assertEquals(fatiha1, repository.observeProgress().first()?.ref)

        repository.saveProgress(baqarah255)
        assertEquals(baqarah255, repository.observeProgress().first()?.ref)
        // A single row survives: the second save replaced the first.
        assertEquals(297, progressDao.rows.value?.globalAyahNumber)
    }

    @Test
    fun clearProgress_emptiesTheStoredPosition() = runTest {
        val progressDao = FakeQuranReadingProgressDao()
        val repository = repository(progressDao = progressDao)

        repository.saveProgress(fatiha1)
        repository.clearProgress()

        assertNull(repository.observeProgress().first())
    }

    private fun repository(
        bookmarkDao: QuranBookmarkDao = FakeQuranBookmarkDao(),
        progressDao: QuranReadingProgressDao = FakeQuranReadingProgressDao(),
        clock: () -> Long = { 0L },
    ) = QuranUserStateRepository(
        bookmarkDao = bookmarkDao,
        readingProgressDao = progressDao,
        clock = clock,
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}

private class FakeQuranBookmarkDao : QuranBookmarkDao {

    val rows = MutableStateFlow<List<QuranBookmarkEntity>>(emptyList())
    var clock: Long = 0L

    override fun observeAll(): Flow<List<QuranBookmarkEntity>> = rows

    override fun observeAyahNumbersOfSurah(surahNumber: Int): Flow<List<Int>> =
        rows.map { list ->
            list.filter { it.surahNumber == surahNumber }.map { it.ayahNumberInSurah }
        }

    override suspend fun exists(globalAyahNumber: Int): Boolean =
        rows.value.any { it.globalAyahNumber == globalAyahNumber }

    override suspend fun deleteByGlobalNumber(globalAyahNumber: Int) {
        rows.value = rows.value.filterNot { it.globalAyahNumber == globalAyahNumber }
    }

    override suspend fun insert(entity: QuranBookmarkEntity): Long {
        if (rows.value.any { it.globalAyahNumber == entity.globalAyahNumber }) return -1L
        rows.value = rows.value + entity
        return entity.globalAyahNumber.toLong()
    }
}

private class FakeQuranReadingProgressDao : QuranReadingProgressDao {

    val rows = MutableStateFlow<QuranReadingProgressEntity?>(null)

    override fun observe(id: Int): Flow<QuranReadingProgressEntity?> = rows

    override suspend fun upsert(entity: QuranReadingProgressEntity) {
        rows.value = entity
    }

    override suspend fun clear() {
        rows.value = null
    }
}
