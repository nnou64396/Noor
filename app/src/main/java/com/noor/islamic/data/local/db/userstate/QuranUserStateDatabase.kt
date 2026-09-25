package com.noor.islamic.data.local.db.userstate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * App-private database for mutable Phase 4 user state (bookmarks and reading
 * progress).
 *
 * It is a separate file from the authoritative `quran_reader.db` asset so the Quran
 * corpus and its schema can never be altered by user data.
 */
@Database(
    entities = [QuranBookmarkEntity::class, QuranReadingProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuranUserStateDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): QuranBookmarkDao

    abstract fun readingProgressDao(): QuranReadingProgressDao

    companion object {
        private const val DB_NAME = "quran_user_state.db"

        fun create(context: Context): QuranUserStateDatabase =
            Room.databaseBuilder(context, QuranUserStateDatabase::class.java, DB_NAME)
                .build()
    }
}
