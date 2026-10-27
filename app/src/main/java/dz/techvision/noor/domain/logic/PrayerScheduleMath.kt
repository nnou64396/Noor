package dz.techvision.noor.domain.logic

import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.PrayerName
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure, testable prayer-calendar logic: next-prayer selection and countdown.
 *
 * Times are compared in the schedule's own timezone (the date and DST
 * handling are delegated to [ZonedDateTime]); the callers pass a
 * `now` value already expressed in that zone.
 */
object PrayerScheduleMath {

    data class NextPrayer(
        val prayer: PrayerName,
        val date: LocalDate,
        val time: LocalTime,
    )

    /**
     * Returns the first prayer that has not yet occurred, scanning today's
     * schedule first (in canonical order) and then — once Isha has passed —
     * tomorrow's Fajr. Returns null when no future prayer is available
     * (e.g. after Isha with no tomorrow schedule cached).
     *
     * Only obligatory prayers are considered. Sunrise and Sunset are part of
     * the daily timings the UI displays, but they are solar events rather than
     * prayers, so between Fajr and Dhuhr the next prayer is Dhuhr (not
     * Sunrise), and between Asr and Maghrib it is Maghrib (not Sunset).
     */
    fun nextPrayer(
        today: DailyPrayerSchedule,
        tomorrow: DailyPrayerSchedule?,
        now: ZonedDateTime,
    ): NextPrayer? {
        val zone = now.zone

        for (prayerTime in today.prayers) {
            if (!prayerTime.prayer.isObligatoryPrayer) continue
            val at = ZonedDateTime.of(today.date, prayerTime.time, zone)
            if (at.isAfter(now)) {
                return NextPrayer(prayerTime.prayer, today.date, prayerTime.time)
            }
        }

        val tomorrowFajr = tomorrow?.prayers
            ?.firstOrNull { it.prayer.isObligatoryPrayer }
            ?: return null
        return NextPrayer(tomorrowFajr.prayer, tomorrow.date, tomorrowFajr.time)
    }

    /** Non-negative time remaining until [next], expressed in [now]'s zone. */
    fun countdown(now: ZonedDateTime, next: NextPrayer): Duration {
        val at = ZonedDateTime.of(next.date, next.time, now.zone)
        return Duration.between(now, at).coerceAtLeast(Duration.ZERO)
    }
}
