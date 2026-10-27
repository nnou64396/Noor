package dz.techvision.noor.data.local.db.userstate

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One bookmarked ayah, identified by the canonical global ayah number. The displayed
 * Arabic text is never stored, so the bookmark identity is independent of the corpus text.
 */
@Entity(
    tableName = "quran_bookmarks",
    indices = [Index(value = ["surah_number", "ayah_number_in_surah"], unique = true)],
)
data class QuranBookmarkEntity(
    @PrimaryKey @ColumnInfo(name = "global_ayah_number") val globalAyahNumber: Int,
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "ayah_number_in_surah") val ayahNumberInSurah: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface QuranBookmarkDao {

    @Query("SELECT * FROM quran_bookmarks ORDER BY created_at DESC, global_ayah_number DESC")
    fun observeAll(): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT ayah_number_in_surah FROM quran_bookmarks WHERE surah_number = :surahNumber")
    fun observeAyahNumbersOfSurah(surahNumber: Int): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM quran_bookmarks WHERE global_ayah_number = :globalAyahNumber)")
    suspend fun exists(globalAyahNumber: Int): Boolean

    @Query("DELETE FROM quran_bookmarks WHERE global_ayah_number = :globalAyahNumber")
    suspend fun deleteByGlobalNumber(globalAyahNumber: Int)

    /** Returns -1 when the ayah is already bookmarked (the unique index rejects it). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: QuranBookmarkEntity): Long
}

