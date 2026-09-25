package com.noor.islamic.data.local.db.quran

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Read-only Quran reader database backed by the prebuilt asset
 * `assets/quran/quran_reader.db`. Schema is identical to the exported
 * Room schema (same identity hash), so the asset opens exactly as if Room
 * had built it. exportSchema=true feeds the data importer in tools/quran.
 */
@Database(
    entities = [SurahEntity::class, AyahEntity::class, AyahFtsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class QuranReaderDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        private const val ASSET_PATH = "quran/quran_reader.db"
        private const val DB_NAME = "quran_reader.db"

        fun create(context: Context): QuranReaderDatabase =
            Room.databaseBuilder(context, QuranReaderDatabase::class.java, DB_NAME)
                .createFromAsset(ASSET_PATH)
                .build()
    }
}