package com.noor.islamic.data.local.db

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.model.PrayerTime
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure mapper between the Room entity and the domain schedule. Kept out of
 * the DAO so it can be unit-tested in isolation.
 */
object PrayerEntityMapper {

    private val timePattern = Regex("(\\d{1,2}):(\\d{2})")

    fun toEntity(
        schedule: DailyPrayerSchedule,
        fetchedAtEpochMillis: Long = System.currentTimeMillis(),
    ): PrayerTimesEntity {
        fun time(prayer: PrayerName): String =
            schedule.prayers.first { it.prayer == prayer }.time.toString()

        return PrayerTimesEntity(
            cacheKey = "",
            dateIso = schedule.date.toString(),
            hijriReadable = schedule.hijriReadable,
            timezone = schedule.timezone,
            methodId = schedule.method.apiId,
            fajr = time(PrayerName.FAJR),
            sunrise = time(PrayerName.SUNRISE),
            dhuhr = time(PrayerName.DHUHR),
            asr = time(PrayerName.ASR),
            sunset = time(PrayerName.SUNSET),
            maghrib = time(PrayerName.MAGHRIB),
            isha = time(PrayerName.ISHA),
            fetchedAtEpochMillis = fetchedAtEpochMillis,
        )
    }

    fun toDomain(entity: PrayerTimesEntity): DailyPrayerSchedule? {
        val date = try {
            LocalDate.parse(entity.dateIso)
        } catch (_: Exception) {
            return null
        }
        val method = CalculationMethod.entries.firstOrNull { it.apiId == entity.methodId }
            ?: CalculationMethod.MUSLIM_WORLD_LEAGUE
        val times = listOf(
            entity.fajr to PrayerName.FAJR,
            entity.sunrise to PrayerName.SUNRISE,
            entity.dhuhr to PrayerName.DHUHR,
            entity.asr to PrayerName.ASR,
            entity.sunset to PrayerName.SUNSET,
            entity.maghrib to PrayerName.MAGHRIB,
            entity.isha to PrayerName.ISHA,
        )
        val prayers = times.mapNotNull { (raw, name) ->
            parseTime(raw)?.let { PrayerTime(name, it) }
        }
        if (prayers.size != times.size) return null

        return DailyPrayerSchedule(
            date = date,
            hijriReadable = entity.hijriReadable,
            timezone = entity.timezone,
            method = method,
            prayers = prayers,
        )
    }

    private fun parseTime(raw: String): LocalTime? {
        val match = timePattern.find(raw) ?: return null
        return try {
            LocalTime.of(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
            )
        } catch (_: Exception) {
            null
        }
    }
}