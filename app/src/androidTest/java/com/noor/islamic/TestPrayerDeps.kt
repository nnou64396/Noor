package com.noor.islamic

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.PrayerDataException
import com.noor.islamic.domain.model.PrayerErrorKind
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.model.PrayerTime
import com.noor.islamic.domain.repository.LocationPermissionChecker
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.repository.PrayerRepository
import java.time.LocalDate
import java.time.LocalTime

/** Deterministic fakes so instrumented tests render without hardware/network. */

class NotGrantedPermissionChecker : LocationPermissionChecker {
    override fun isGranted() = false
    override fun shouldShowRationale() = false
}

class GrantedPermissionChecker : LocationPermissionChecker {
    override fun isGranted() = true
    override fun shouldShowRationale() = false
}

class FakeLocationRepository(
    private val result: Result<GeographicPoint> = Result.success(GeographicPoint(36.75, 3.05)),
) : LocationRepository {
    override suspend fun obtainLocation(): Result<GeographicPoint> = result
    override suspend fun cityLabel(point: GeographicPoint): String? = "Alger"
}

class FakePrayerRepository(
    private val failRefresh: Boolean = false,
) : PrayerRepository {
    private val sample = DailyPrayerSchedule(
        date = LocalDate.of(2026, 9, 24),
        hijriReadable = "12 Rabi' al-awwal 1448",
        timezone = "Africa/Algiers",
        method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        latitude = 36.75,
        longitude = 3.05,
        prayers = listOf(
            PrayerTime(PrayerName.FAJR, LocalTime.of(5, 20)),
            PrayerTime(PrayerName.SUNRISE, LocalTime.of(6, 20)),
            PrayerTime(PrayerName.DHUHR, LocalTime.of(12, 44)),
            PrayerTime(PrayerName.ASR, LocalTime.of(16, 30)),
            PrayerTime(PrayerName.SUNSET, LocalTime.of(19, 40)),
            PrayerTime(PrayerName.MAGHRIB, LocalTime.of(19, 40)),
            PrayerTime(PrayerName.ISHA, LocalTime.of(21, 0)),
        ),
    )

    override suspend fun getCached(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule? = sample.copy(date = date)

    override suspend fun refresh(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule {
        if (failRefresh) throw PrayerDataException(PrayerErrorKind.NETWORK)
        return getCached(date, point, method)!!
    }
}