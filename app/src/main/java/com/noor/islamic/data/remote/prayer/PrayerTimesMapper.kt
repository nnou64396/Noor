package com.noor.islamic.data.remote.prayer

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.model.PrayerTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Pure mapper: AlAdhan DTO -> [DailyPrayerSchedule].
 *
 * Returns null (treated as a malformed response by the repository) when any
 * canonical timing, the timezone or the Hijri date is missing/unparseable.
 * API-specific formatting never leaks into the UI.
 *
 * The response's `meta` coordinates are intentionally not copied into the
 * domain model: the location is a transient request input and is dropped here
 * so it cannot be persisted downstream.
 */
class PrayerTimesMapper {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun map(
        response: TimingsResponse,
        requestedDate: LocalDate,
    ): DailyPrayerSchedule? {
        val data = response.data ?: return null
        val meta = data.meta ?: return null
        val timezone = meta.timezone ?: return null
        val timings = data.timings ?: return null

        val zone = try {
            ZoneId.of(timezone)
        } catch (_: Exception) {
            return null
        }

        val prayers = ArrayList<PrayerTime>(PrayerName.entries.size)
        for (name in PrayerName.entries) {
            val raw = timings[name.apiKey] ?: return null
            val time = try {
                LocalTime.parse(raw, timeFormatter)
            } catch (_: DateTimeParseException) {
                return null
            }
            prayers += PrayerTime(name, time)
        }

        return DailyPrayerSchedule(
            date = requestedDate,
            hijriReadable = data.date?.hijri?.readable,
            timezone = timezone,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            prayers = prayers,
        )
    }
}