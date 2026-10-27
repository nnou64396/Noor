package dz.techvision.noor.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrayerTimesDao {

    @Query("SELECT * FROM prayer_times WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): PrayerTimesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PrayerTimesEntity)

    /** Removes entries older than [cutoffMillis] (no naive cleanup here). */
    @Query("DELETE FROM prayer_times WHERE fetchedAtEpochMillis < :cutoffMillis")
    suspend fun pruneBefore(cutoffMillis: Long)
}
