package com.noor.islamic.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Noor Room database. Phase 0 reserved the name `noor.db`; the first real
 * table (prayer-times cache) now ships with schema version 1.
 */
@Database(
    entities = [PrayerTimesEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NoorDatabase : RoomDatabase() {

    abstract fun prayerTimesDao(): PrayerTimesDao

    companion object {
        private const val DB_NAME = "noor.db"

        fun create(context: Context): NoorDatabase =
            Room.databaseBuilder(context, NoorDatabase::class.java, DB_NAME)
                .build()
    }
}