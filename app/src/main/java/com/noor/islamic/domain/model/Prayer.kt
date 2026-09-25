package com.noor.islamic.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * The canonical daily prayer sequence. The enum order matches the
 * chronological order used by the AlAdhan timings response, which also
 * defines the order shown in the UI and used by the countdown logic.
 */
enum class PrayerName(val apiKey: String) {
    FAJR("Fajr"),
    SUNRISE("Sunrise"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    SUNSET("Sunset"),
    MAGHRIB("Maghrib"),
    ISHA("Isha"),
}

/** A single prayer timing. */
data class PrayerTime(
    val prayer: PrayerName,
    val time: LocalTime,
)

/**
 * The full set of timings for one (gregorian) date, together with the
 * metadata that identifies how the schedule was calculated.
 */
data class DailyPrayerSchedule(
    val date: LocalDate,
    /** Readable Hijri date supplied by the API (e.g. "12 Rabi' al-awwal 1448"). */
    val hijriReadable: String?,
    /** IANA timezone the timings are expressed in (e.g. "Africa/Algiers"). */
    val timezone: String,
    val method: CalculationMethod,
    val latitude: Double,
    val longitude: Double,
    val prayers: List<PrayerTime>,
) {
    /** True when every one of the seven canonical prayers is present. */
    val isComplete: Boolean
        get() = PrayerName.entries.all { name -> prayers.any { it.prayer == name } }
}