package dz.techvision.noor.data.local.db.userstate

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Single-row table holding the latest reading position. Only the most recent
 * surah/ayah is needed to resume reading, so nothing more is stored.
 */
@Entity(tableName = "quran_reading_progress")
data class QuranReadingProgressEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "ayah_number_in_surah") val ayahNumberInSurah: Int,
    @ColumnInfo(name = "global_ayah_number") val globalAyahNumber: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface QuranReadingProgressDao {

    @Query("SELECT * FROM quran_reading_progress WHERE id = :id")
    fun observe(id: Int = QuranReadingProgressEntity.SINGLETON_ID): Flow<QuranReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuranReadingProgressEntity)

    @Query("DELETE FROM quran_reading_progress")
    suspend fun clear()
}

