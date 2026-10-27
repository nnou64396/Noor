package dz.techvision.noor

import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.CalculationMethodConfig
import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.GeographicPoint
import dz.techvision.noor.domain.model.PrayerDataException
import dz.techvision.noor.domain.model.PrayerErrorKind
import dz.techvision.noor.domain.model.PrayerName
import dz.techvision.noor.domain.model.PrayerTime
import dz.techvision.noor.domain.repository.LocationPermissionChecker
import dz.techvision.noor.domain.repository.LocationRepository
import dz.techvision.noor.domain.repository.PrayerRepository
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

    override suspend fun getCachedFetchedAt(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): java.time.Instant? = null

    override suspend fun refresh(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule {
        if (failRefresh) throw PrayerDataException(PrayerErrorKind.NETWORK)
        return getCached(date, point, method)!!
    }
}
