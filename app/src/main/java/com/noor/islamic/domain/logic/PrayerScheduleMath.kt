package com.noor.islamic.domain.logic

import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.PrayerName
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
     */
    fun nextPrayer(
        today: DailyPrayerSchedule,
        tomorrow: DailyPrayerSchedule?,
        now: ZonedDateTime,
    ): NextPrayer? {
        val zone = now.zone

        for (prayerTime in today.prayers) {
            val at = ZonedDateTime.of(today.date, prayerTime.time, zone)
            if (at.isAfter(now)) {
                return NextPrayer(prayerTime.prayer, today.date, prayerTime.time)
            }
        }

        val fajr = tomorrow?.prayers?.firstOrNull() ?: return null
        return NextPrayer(fajr.prayer, tomorrow.date, fajr.time)
    }

    /** Non-negative time remaining until [next], expressed in [now]'s zone. */
    fun countdown(now: ZonedDateTime, next: NextPrayer): Duration {
        val at = ZonedDateTime.of(next.date, next.time, now.zone)
        return Duration.between(now, at).coerceAtLeast(Duration.ZERO)
    }
}