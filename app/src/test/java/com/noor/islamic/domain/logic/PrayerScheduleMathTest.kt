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
    fun `sunrise is never reported as the next prayer`() {
        // 05:30 sits between Fajr (05:20) and Sunrise (06:20). Sunrise is a solar
        // event, not a prayer, so the next prayer must be Dhuhr.
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 5, 30))
        assertEquals(PrayerName.DHUHR, next?.prayer)
        assertEquals(LocalTime.of(12, 44), next?.time)
    }

    @Test
    fun `next prayer immediately after sunrise is still dhuhr`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 6, 20))
        assertEquals(PrayerName.DHUHR, next?.prayer)
        assertEquals(day, next?.date)
        assertEquals(LocalTime.of(12, 44), next?.time)
    }

    @Test
    fun `sunset is never reported as the next prayer`() {
        // 18:00 sits between Asr (16:30) and Sunset (19:40).
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 18, 0))
        assertEquals(PrayerName.MAGHRIB, next?.prayer)
        assertEquals(LocalTime.of(19, 40), next?.time)
    }

    @Test
    fun `next prayer between maghrib and isha is isha`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 20, 0))
        assertEquals(PrayerName.ISHA, next?.prayer)
        assertEquals(LocalTime.of(21, 0), next?.time)
    }

    @Test
    fun `exactly at a prayer time advances to the following prayer`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 12, 44))
        assertEquals(PrayerName.ASR, next?.prayer)
    }

    @Test
    fun `next prayer after isha on the same day is tomorrow's fajr`() {
        val next = PrayerScheduleMath.nextPrayer(schedule(), schedule(day.plusDays(1)), at(day, 23, 30))
        assertEquals(PrayerName.FAJR, next?.prayer)
        assertEquals(day.plusDays(1), next?.date)
    }

    @Test
    fun `tomorrow's first entry is used only when it is an obligatory prayer`() {
        // A tomorrow schedule whose first entry is Sunrise must still yield Fajr.
        val tomorrowWithSunriseFirst = listOf(
            PrayerTime(PrayerName.SUNRISE, LocalTime.of(6, 10)),
            PrayerTime(PrayerName.FAJR, LocalTime.of(5, 10)),
        )
        val next = PrayerScheduleMath.nextPrayer(
            schedule(),
            schedule(day.plusDays(1), tomorrowWithSunriseFirst),
            at(day, 22, 0),
        )
        assertEquals(PrayerName.FAJR, next?.prayer)
        assertEquals(LocalTime.of(5, 10), next?.time)
    }

    @Test
    fun `obligatoryPrayers excludes the solar events`() {
        assertEquals(
            listOf(
                PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR,
                PrayerName.MAGHRIB, PrayerName.ISHA,
            ),
            PrayerName.obligatoryPrayers,
        )
        assertEquals(false, PrayerName.SUNRISE.isObligatoryPrayer)
        assertEquals(false, PrayerName.SUNSET.isObligatoryPrayer)
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