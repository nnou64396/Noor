package com.noor.islamic.domain.logic

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.model.PrayerTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerScheduleMathTest {

    private val zone = ZoneId.of("Africa/Algiers")
    private val day = LocalDate.of(2026, 9, 24)

    private val todayTimes = listOf(
        PrayerTime(PrayerName.FAJR, LocalTime.of(5, 20)),
        PrayerTime(PrayerName.SUNRISE, LocalTime.of(6, 20)),
        PrayerTime(PrayerName.DHUHR, LocalTime.of(12, 44)),
        PrayerTime(PrayerName.ASR, LocalTime.of(16, 30)),
        PrayerTime(PrayerName.SUNSET, LocalTime.of(19, 40)),
        PrayerTime(PrayerName.MAGHRIB, LocalTime.of(19, 40)),
        PrayerTime(PrayerName.ISHA, LocalTime.of(21, 0)),
    )

    private fun schedule(
        date: LocalDate = day,
        times: List<PrayerTime> = todayTimes,
    ) = DailyPrayerSchedule(
        date = date,
        hijriReadable = null,
        timezone = zone.id,
        method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        latitude = 36.75,
        longitude = 3.05,
        prayers = times,
    )

    private fun at(date: LocalDate, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)

    @Test
    fun `next prayer before fajr is today's fajr`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 4, 50))
        assertEquals(PrayerName.FAJR, next?.prayer)
        assertEquals(day, next?.date)
        assertEquals(LocalTime.of(5, 20), next?.time)
    }

    @Test
    fun `next prayer between dhuhr and asr is asr`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 13, 30))
        assertEquals(PrayerName.ASR, next?.prayer)
        assertEquals(LocalTime.of(16, 30), next?.time)
    }

    @Test
    fun `after isha picks fajr of the next day`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 21, 30))
        assertEquals(PrayerName.FAJR, next?.prayer)
        assertEquals(day.plusDays(1), next?.date)
        assertEquals(LocalTime.of(5, 20), next?.time)
    }

    @Test
    fun `after isha without tomorrow schedule returns null`() {
        assertNull(PrayerScheduleMath.nextPrayer(schedule(), null, at(day, 22, 0)))
    }

    @Test
    fun `midnight boundary rolls to the new day's fajr`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 23, 59))
        assertEquals(day.plusDays(1), next?.date)
        assertEquals(PrayerName.FAJR, next?.prayer)
    }

    @Test
    fun `countdown to next prayer is the exact elapsed time`() {
        val next = PrayerScheduleMath.NextPrayer(PrayerName.DHUHR, day, LocalTime.of(12, 44))
        val remaining = PrayerScheduleMath.countdown(at(day, 12, 0), next)
        assertEquals(Duration.ofMinutes(44), remaining)
    }

    @Test
    fun `countdown is clamped to zero in the past`() {
        val next = PrayerScheduleMath.NextPrayer(PrayerName.ASR, day, LocalTime.of(12, 44))
        val remaining = PrayerScheduleMath.countdown(at(day, 13, 0), next)
        assertEquals(Duration.ZERO, remaining)
    }

    @Test
    fun `countdown respects the schedule timezone across dst-like offsets`() {
        val next = PrayerScheduleMath.NextPrayer(PrayerName.FAJR, day, LocalTime.of(5, 20))
        val now = at(day, 4, 50)
        assertEquals(Duration.ofMinutes(30), PrayerScheduleMath.countdown(now, next))
    }
}