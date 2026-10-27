package dz.techvision.noor.data.local.db.practice

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Repetition progress for one dhikr, keyed by the corpus-assigned stable id.
 *
 * Only the counter is stored: the dhikr text always comes from the read-only
 * corpus, so a corpus update can never be shadowed by stale user state.
 */
@Entity(tableName = "athkar_dhikr_progress")
data class AthkarDhikrProgressEntity(
    @PrimaryKey @ColumnInfo(name = "dhikr_id") val dhikrId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "completed_repetitions") val completedRepetitions: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface AthkarProgressDao {

    @Query("SELECT * FROM athkar_dhikr_progress")
    fun observeAll(): Flow<List<AthkarDhikrProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AthkarDhikrProgressEntity)

    @Query("DELETE FROM athkar_dhikr_progress WHERE dhikr_id = :dhikrId")
    suspend fun deleteByDhikrId(dhikrId: String)

    @Query("DELETE FROM athkar_dhikr_progress WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    @Query("DELETE FROM athkar_dhikr_progress")
    suspend fun deleteAll()
}

