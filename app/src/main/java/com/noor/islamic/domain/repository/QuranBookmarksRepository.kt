package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.Bookmark
import kotlinx.coroutines.flow.Flow

/**
 * Local Quran bookmarks. Backed by an app-private Room table that is completely
 * separate from the authoritative offline Quran database.
 */
interface QuranBookmarksRepository {

    fun observeBookmarks(): Flow<List<Bookmark>>

    /** Ayah numbers (within the surah) that are bookmarked, for reader highlighting. */
    fun observeBookmarkedAyahNumbers(surahNumber: Int): Flow<Set<Int>>

    /** Returns true when the ayah was newly bookmarked, false when it already existed. */
    suspend fun add(ref: AyahRef): Boolean

    suspend fun remove(ref: AyahRef)

    suspend fun isBookmarked(ref: AyahRef): Boolean
}
