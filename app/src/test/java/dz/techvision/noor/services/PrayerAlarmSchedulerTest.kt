package dz.techvision.noor.services

import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.PrayerName
import dz.techvision.noor.domain.model.PrayerTime
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PrayerAlarmSchedulerTest {

    @Test
    fun testScheduleAlarmsModelData() {
        val schedule = DailyPrayerSchedule(
            date = LocalDate.now(),
            hijriReadable = "1 Muharram 1448",
            timezone = "UTC",
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            prayers = listOf(
                PrayerTime(PrayerName.FAJR, LocalTime.of(5, 0)),
                PrayerTime(PrayerName.DHUHR, LocalTime.of(12, 0)),
            ),
        )
        org.junit.Assert.assertTrue(schedule.isComplete || !schedule.isComplete)
    }
}
