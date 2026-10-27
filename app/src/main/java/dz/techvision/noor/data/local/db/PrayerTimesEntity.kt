package dz.techvision.noor.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached daily prayer schedule. One row per (date, location, method).
 *
 * Holds no coordinates. The primary key is a SHA-256 digest of the date, a
 * coarse location and the method, so the device's latitude/longitude is used
 * only to build that key and to issue the request, then discarded. A precise
 * location cannot be read out of this table.
 *
 * The digest is still location-adjacent (see `PrayerCacheKey`), which is why
 * `noor.db` is excluded from backup and device transfer.
 */
@Entity(tableName = "prayer_times")
data class PrayerTimesEntity(
    @PrimaryKey val cacheKey: String,
    /** Gregorian date as `yyyy-MM-dd`. */
    val dateIso: String,
    val hijriReadable: String?,
    val timezone: String,
    val methodId: Int,
    /** Times stored as `HH:mm` in the schedule timezone. */
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val sunset: String,
    val maghrib: String,
    val isha: String,
    val fetchedAtEpochMillis: Long,
)
