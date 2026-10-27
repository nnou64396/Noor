package dz.techvision.noor.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * The canonical daily prayer sequence. The enum order matches the
 * chronological order used by the AlAdhan timings response, which also
 * defines the order shown in the UI and used by the countdown logic.
 *
 * [isObligatoryPrayer] separates the five timed prayers from the two solar
 * events (Sunrise and Sunset). Sunrise and Sunset are timings the UI still
 * lists, but they are not prayers, so they must never be reported as the
 * "next prayer" the user is counting towards.
 */
enum class PrayerName(
    val apiKey: String,
    val isObligatoryPrayer: Boolean,
) {
    FAJR("Fajr", true),
    SUNRISE("Sunrise", false),
    DHUHR("Dhuhr", true),
    ASR("Asr", true),
    SUNSET("Sunset", false),
    MAGHRIB("Maghrib", true),
    ISHA("Isha", true);

    companion object {
        /** The five prayers a "next prayer" countdown may refer to. */
        val obligatoryPrayers: List<PrayerName> = entries.filter { it.isObligatoryPrayer }
    }
}

/** A single prayer timing. */
data class PrayerTime(
    val prayer: PrayerName,
    val time: LocalTime,
)

/**
 * The full set of timings for one (gregorian) date, together with the
 * metadata that identifies how the schedule was calculated.
 *
 * Deliberately carries no coordinates. The device location is a transient
 * input to the network request and is discarded as soon as the timings come
 * back, so no precise latitude/longitude can reach the prayer cache.
 */
data class DailyPrayerSchedule(
    val date: LocalDate,
    /** Readable Hijri date supplied by the API (e.g. "12 Rabi' al-awwal 1448"). */
    val hijriReadable: String?,
    /** IANA timezone the timings are expressed in (e.g. "Africa/Algiers"). */
    val timezone: String,
    val method: CalculationMethod,
    val prayers: List<PrayerTime>,
) {
    /** True when every one of the seven canonical prayers is present. */
    val isComplete: Boolean
        get() = PrayerName.entries.all { name -> prayers.any { it.prayer == name } }
}
