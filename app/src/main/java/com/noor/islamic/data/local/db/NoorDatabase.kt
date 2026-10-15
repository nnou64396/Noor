package com.noor.islamic.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Noor Room database. Phase 0 reserved the name `noor.db`; the first real
 * table (prayer-times cache) now ships with schema version 2.
 *
 * Version 2 removed the `latitude`/`longitude` columns so the prayer cache can
 * no longer hold a precise device location.
 */
@Database(
    entities = [PrayerTimesEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class NoorDatabase : RoomDatabase() {

    abstract fun prayerTimesDao(): PrayerTimesDao

    companion object {
        private const val DB_NAME = "noor.db"

        /**
         * Version 1 stored the device's precise coordinates in two columns and
         * embedded the rounded ones in the primary key. Those rows cannot be
         * rewritten in place without carrying the coordinates forward, so the
         * table is dropped and recreated empty.
         *
         * Dropping also erases the stored coordinates, which is the point of the
         * change. The cost is one refetch of today's timings, because the cache
         * is a network optimisation rather than user data.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `prayer_times`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prayer_times` (
                        `cacheKey` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `hijriReadable` TEXT,
                        `timezone` TEXT NOT NULL,
                        `methodId` INTEGER NOT NULL,
                        `fajr` TEXT NOT NULL,
                        `sunrise` TEXT NOT NULL,
                        `dhuhr` TEXT NOT NULL,
                        `asr` TEXT NOT NULL,
                        `sunset` TEXT NOT NULL,
                        `maghrib` TEXT NOT NULL,
                        `isha` TEXT NOT NULL,
                        `fetchedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun create(context: Context): NoorDatabase =
            Room.databaseBuilder(context, NoorDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
