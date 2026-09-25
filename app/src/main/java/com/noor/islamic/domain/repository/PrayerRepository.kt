package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import java.time.LocalDate

/**
 * Offline-first source for daily prayer timings: a Room cache backed by
 * the AlAdhan remote API.
 */
interface PrayerRepository {

    /**
     * Reads the latest cached schedule for [date] at [point] + [method],
     * or null when nothing is cached. Fast, no network.
     */
    suspend fun getCached(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule?

    /**
     * Fetches a fresh schedule for [date] from the API and stores it in the
     * cache before returning. Throws a typed [com.noor.islamic.domain.model.PrayerDataException]
     * on every failure; the cache is left untouched in that case so cached
     * data stays usable while offline.
     */
    suspend fun refresh(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule
}