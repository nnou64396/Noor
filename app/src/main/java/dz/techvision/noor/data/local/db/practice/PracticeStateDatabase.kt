package dz.techvision.noor.data.local.db.practice

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * App-private database for Phase 5 daily-practice state: adhkar repetition
 * counters and the Tasbih session.
 *
 * It is a separate file from the authoritative `quran_reader.db` asset (which
 * stays read-only and byte-identical) and from the Phase 4 `quran_user_state.db`,
 * so a change to this schema can never migrate or invalidate Quran bookmarks and
 * reading progress.
 */
@Database(
    entities = [AthkarDhikrProgressEntity::class, TasbihSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PracticeStateDatabase : RoomDatabase() {

    abstract fun athkarProgressDao(): AthkarProgressDao

    abstract fun tasbihDao(): TasbihDao

    companion object {
        private const val DB_NAME = "noor_practice_state.db"

        fun create(context: Context): PracticeStateDatabase =
            Room.databaseBuilder(context, PracticeStateDatabase::class.java, DB_NAME)
                .build()
    }
}

