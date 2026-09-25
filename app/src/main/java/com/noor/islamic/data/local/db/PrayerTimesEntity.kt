package com.noor.islamic.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached daily prayer schedule. One row per (date, coarse location, method);
 * pruned aggressively so no location trail accumulates.
 */
@Entity(tableName = "prayer_times")
data class PrayerTimesEntity(
    @PrimaryKey val cacheKey: String,
    /** Gregorian date as `yyyy-MM-dd`. */
    val dateIso: String,
    val hijriReadable: String?,
    val timezone: String,
    val methodId: Int,
    val latitude: Double,
    val longitude: Double,
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