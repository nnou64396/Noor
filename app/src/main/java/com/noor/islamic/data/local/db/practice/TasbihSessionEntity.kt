package com.noor.islamic.data.local.db.practice

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The single Tasbih session. A fixed id keeps it a genuine singleton row, so
 * there is never more than one session to reconcile on restore.
 */
@Entity(tableName = "tasbih_session")
data class TasbihSessionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "target") val target: Int,
    @ColumnInfo(name = "total_count") val totalCount: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface TasbihDao {

    @Query("SELECT * FROM tasbih_session WHERE id = :id")
    fun observe(id: Int = TasbihSessionEntity.SINGLETON_ID): Flow<TasbihSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TasbihSessionEntity)

    @Query("DELETE FROM tasbih_session")
    suspend fun deleteAll()
}
