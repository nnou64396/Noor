package dz.techvision.noor.presentation.prayer

import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.CalculationMethodConfig
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.GeographicPoint
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.PrayerDataException
import dz.techvision.noor.domain.model.PrayerErrorKind
import dz.techvision.noor.domain.model.PrayerName
import dz.techvision.noor.domain.model.PrayerTime
import dz.techvision.noor.domain.repository.LocationPermissionChecker
import dz.techvision.noor.domain.repository.LocationRepository
import dz.techvision.noor.domain.repository.PrayerRepository
import dz.techvision.noor.domain.repository.SettingsRepository
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.presentation.common.LocationPermissionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Prayer ViewModel over fake collaborators: permission flow, offline-first
 * cache fallback, and the guarantee that a failure always terminates the
 * loading state instead of spinning.
 */
class PrayerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val clock = Clock.fixed(Instant.parse("2026-09-27T09:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // --- Permission flow -----------------------------------------------------

    @Test
    fun `a granted permission loads the schedule on construction`() = runTest(dispatcher) {
        val prayers = FakePrayerRepository()
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationPermissionUiState.GRANTED, state.permission)
        assertFalse(state.isLocating)
        val ready = state.schedule as PrayerScheduleUi.Ready
        assertFalse(ready.fromCache)
        assertEquals(today, ready.today.date)
    }

    @Test
    fun `an unrequested permission never asks for a location`() = runTest(dispatcher) {
        val location = FakeLocationRepository(success = true)
        val viewModel = viewModel(location = location, granted = false)
        advanceUntilIdle()

        assertEquals(LocationPermissionUiState.NOT_REQUESTED, viewModel.uiState.value.permission)
        assertEquals(0, location.obtainCount)
        assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Idle)
    }

    @Test
    fun `granting the permission afterwards starts the load`() = runTest(dispatcher) {
        val location = FakeLocationRepository(success = true)
        val viewModel = viewModel(location = location, granted = false)
        advanceUntilIdle()

        viewModel.onPermissionGranted()
        advanceUntilIdle()

        assertEquals(LocationPermissionUiState.GRANTED, viewModel.uiState.value.permission)
        assertEquals(1, location.obtainCount)
        assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Ready)
    }

    @Test
    fun `a denial that still allows a rationale stays retryable`() = runTest(dispatcher) {
        val viewModel = viewModel(granted = false, rationale = true)
        advanceUntilIdle()

        viewModel.onPermissionDenied()
        advanceUntilIdle()

        assertEquals(LocationPermissionUiState.DENIED_RETRYABLE, viewModel.uiState.value.permission)
        assertTrue(viewModel.uiState.value.hasRequestedPermission)
    }

    @Test
    fun `a denial without a rationale is reported as permanent`() = runTest(dispatcher) {
        val viewModel = viewModel(granted = false, rationale = false)
        advanceUntilIdle()

        viewModel.onPermissionDenied()
        advanceUntilIdle()

        assertEquals(LocationPermissionUiState.DENIED_PERMANENT, viewModel.uiState.value.permission)
    }

    @Test
    fun `a permission granted in system settings is picked up on the next resume`() =
        runTest(dispatcher) {
            // Models returning from the Settings screen: the checker now reports
            // granted, but the ViewModel was created while it was still denied.
            val checker = FakePermissionChecker(granted = false)
            val location = FakeLocationRepository(success = true)
            val viewModel = PrayerViewModel(
                permissionChecker = checker,
                locationRepository = location,
                prayerRepository = FakePrayerRepository(),
                clock = clock,
            )
            advanceUntilIdle()
            assertEquals(LocationPermissionUiState.NOT_REQUESTED, viewModel.uiState.value.permission)

            checker.granted = true
            viewModel.onScreenEntered()
            advanceUntilIdle()

            assertEquals(LocationPermissionUiState.GRANTED, viewModel.uiState.value.permission)
            assertEquals(1, location.obtainCount)
            assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Ready)
        }

    // --- Location failures ---------------------------------------------------

    @Test
    fun `an unavailable fix ends locating and shows the error state`() = runTest(dispatcher) {
        val viewModel = viewModel(location = FakeLocationRepository(success = false), granted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("A failed fix must not leave the UI locating", state.isLocating)
        assertNull(state.locationLabel)
        assertTrue(state.schedule is PrayerScheduleUi.Failed)
    }

    @Test
    fun `a successful fix clears the locating indicator and shows the place name`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                location = FakeLocationRepository(success = true, label = "Algiers"),
                granted = true,
            )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLocating)
            assertEquals("Algiers", viewModel.uiState.value.locationLabel)
        }

    @Test
    fun `manual location loads without permission and switching back uses automatic location`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository().apply {
                locationMode.value = LocationMode.MANUAL
                manualLatitude.value = 48.8566
                manualLongitude.value = 2.3522
            }
            val location = FakeLocationRepository(success = true)
            val prayers = FakePrayerRepository()
            val viewModel = PrayerViewModel(
                permissionChecker = FakePermissionChecker(granted = false),
                locationRepository = location,
                prayerRepository = prayers,
                settingsRepository = settings,
                clock = clock,
            )
            advanceUntilIdle()

            assertEquals(LocationMode.MANUAL, viewModel.uiState.value.locationMode)
            assertEquals(0, location.obtainCount)
            assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Ready)
            assertEquals(GeographicPoint(48.8566, 2.3522), prayers.lastPoint)

            settings.setPrayerLocationMode(LocationMode.AUTO)
            advanceUntilIdle()

            assertEquals(LocationMode.AUTO, viewModel.uiState.value.locationMode)
            assertEquals(0, location.obtainCount)
            assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Idle)
        }

    @Test
    fun `switching from manual to automatic requests the device location when permitted`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository().apply {
                locationMode.value = LocationMode.MANUAL
                manualLatitude.value = 48.8566
                manualLongitude.value = 2.3522
            }
            val location = FakeLocationRepository(success = true)
            val prayers = FakePrayerRepository()
            val viewModel = PrayerViewModel(
                permissionChecker = FakePermissionChecker(granted = true),
                locationRepository = location,
                prayerRepository = prayers,
                settingsRepository = settings,
                clock = clock,
            )
            advanceUntilIdle()
            assertEquals(GeographicPoint(48.8566, 2.3522), prayers.lastPoint)

            settings.setPrayerLocationMode(LocationMode.AUTO)
            advanceUntilIdle()

            assertEquals(1, location.obtainCount)
            assertEquals(GeographicPoint(36.7538, 3.0588), prayers.lastPoint)
        }

    @Test
    fun `selected calculation method and Asr school reach the prayer repository`() =
        runTest(dispatcher) {
            val settings = FakeSettingsRepository()
            val prayers = FakePrayerRepository()
            val viewModel = PrayerViewModel(
                permissionChecker = FakePermissionChecker(granted = true),
                locationRepository = FakeLocationRepository(success = true),
                prayerRepository = prayers,
                settingsRepository = settings,
                clock = clock,
            )
            advanceUntilIdle()

            settings.setPrayerCalculationMethod(CalculationMethod.TEHRAN)
            settings.setPrayerAsrSchool(AsrSchool.HANAFI)
            advanceUntilIdle()

            assertEquals(CalculationMethod.TEHRAN, prayers.lastMethod)
            assertEquals(AsrSchool.HANAFI.apiId, prayers.lastSchool)
            assertEquals(GeographicPoint(36.7538, 3.0588), prayers.lastPoint)
        }

    @Test
    fun `an unavailable place name still leaves the schedule loaded`() = runTest(dispatcher) {
        // `cityLabel` is a best-effort reverse geocode; failing to resolve a
        // name must not cost the user their prayer times.
        val viewModel = viewModel(
            location = FakeLocationRepository(success = true, label = null),
            granted = true,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.locationLabel)
        assertTrue(state.schedule is PrayerScheduleUi.Ready)
    }

    // --- Cache / refresh -----------------------------------------------------

    @Test
    fun `a failed refresh falls back to the cached schedule`() = runTest(dispatcher) {
        val prayers = FakePrayerRepository(cached = true, refreshFails = true)
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
        assertTrue("A failed refresh must not destroy valid cached data", ready.fromCache)
        assertEquals(today, ready.today.date)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `a failed refresh with no cache shows the error state`() = runTest(dispatcher) {
        val prayers = FakePrayerRepository(cached = false, refreshFails = true)
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.schedule is PrayerScheduleUi.Failed)
    }

    @Test
    fun `a missing tomorrow schedule is tolerated`() = runTest(dispatcher) {
        val prayers = FakePrayerRepository(tomorrow = false)
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
        assertNull(ready.tomorrow)
    }

    @Test
    fun `a fresh cache for today is served without the network`() = runTest(dispatcher) {
        // Fetched earlier this same (local) day: an immutable schedule for a
        // fixed date is still canonical, so the load must not hit the API again.
        val prayers = FakePrayerRepository(
            cached = true,
            fetchedAt = Instant.parse("2026-09-27T07:00:00Z"),
        )
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
        assertTrue(ready.fromCache)
        assertEquals(today, ready.today.date)
        assertEquals(0, prayers.refreshCount)
    }

    @Test
    fun `a cache fetched on a previous day is refreshed`() = runTest(dispatcher) {
        // 2026-09-26T22:00:00Z is already 27 Sep in Algeria (+01:00), so local
        // midnight separates the fetched timestamp from the requested date.
        val prayers = FakePrayerRepository(
            cached = true,
            fetchedAt = Instant.parse("2026-09-26T22:00:00Z"),
        )
        val viewModel = viewModel(prayers = prayers, granted = true)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
        assertFalse(ready.fromCache)
        assertEquals(2, prayers.refreshCount)
    }

    @Test
    fun `a network failure without cache keeps its kind`() = runTest(dispatcher) {
        val viewModel = viewModel(
            prayers = FakePrayerRepository(refreshFails = true),
            granted = true,
        )
        advanceUntilIdle()

        val failed = viewModel.uiState.value.schedule as PrayerScheduleUi.Failed
        assertEquals(PrayerErrorKind.NETWORK, failed.kind)
    }

    @Test
    fun `an unavailable fix reports a location failure kind`() = runTest(dispatcher) {
        val viewModel = viewModel(
            location = FakeLocationRepository(success = false),
            granted = true,
        )
        advanceUntilIdle()

        val failed = viewModel.uiState.value.schedule as PrayerScheduleUi.Failed
        assertEquals(PrayerErrorKind.LOCATION, failed.kind)
    }

    @Test
    fun `a schedule shown for yesterday reloads for today when the day changes`() =
        runTest(dispatcher) {
            val movingClock = MutableClock(Instant.parse("2026-09-27T09:00:00Z"))
            val prayers = FakePrayerRepository()
            val viewModel = viewModel(prayers = prayers, granted = true, clock = movingClock)
            advanceUntilIdle()
            assertEquals(today, (viewModel.uiState.value.schedule as PrayerScheduleUi.Ready).today.date)

            // Midnight passes while the app stays alive; the next entry to the
            // screen must not keep showing yesterday's schedule.
            movingClock.set(Instant.parse("2026-09-28T00:05:00Z"))
            viewModel.onScreenEntered()
            advanceUntilIdle()

            val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
            assertEquals(LocalDate.of(2026, 9, 28), ready.today.date)
        }

    @Test
    fun `repeated resume does not pile up concurrent location requests`() = runTest(dispatcher) {
        val location = FakeLocationRepository(success = true)
        val prayers = FakePrayerRepository()
        val viewModel = viewModel(location = location, prayers = prayers, granted = true)
        advanceUntilIdle()

        repeat(5) { viewModel.onScreenEntered() }
        viewModel.retry()
        advanceUntilIdle()

        // 1 from construction, then 1 from the explicit retry(). The five
        // resumes in between must add nothing, because permission is already
        // GRANTED and so never transitions, and the in-flight load guard keeps
        // retry() from overlapping a running load.
        assertEquals(2, location.obtainCount)
    }

    @Test
    fun `the next prayer is derived from the loaded schedule`() = runTest(dispatcher) {
        val viewModel = viewModel(granted = true)
        backgroundScope.launch { viewModel.nextPrayer.collect { } }
        // The countdown ticker is an endless loop, so virtual time must only be
        // advanced by hand: advanceUntilIdle() would never settle here.
        runCurrent()

        // The fixed clock is 09:00Z, which is 10:00 in Africa/Algiers. Dhuhr is
        // 12:44, so Dhuhr has not happened yet and is the next prayer (Asr at
        // 16:30 is still further out).
        val next = viewModel.nextPrayer.value as NextPrayerUi.Ready
        assertEquals(PrayerName.DHUHR, next.prayer)
        assertEquals(today, next.date)
        assertEquals(LocalTime.of(12, 44), next.time)
    }

    @Test
    fun `the next prayer stays idle while the schedule is unknown`() = runTest(dispatcher) {
        val viewModel = viewModel(granted = false)
        backgroundScope.launch { viewModel.nextPrayer.collect { } }
        runCurrent()

        assertTrue(viewModel.nextPrayer.value is NextPrayerUi.Idle)
    }

    @Test
    fun `the next prayer falls back to tomorrow's fajr after isha`() = runTest(dispatcher) {
        val afterIsha = Clock.fixed(Instant.parse("2026-09-27T21:30:00Z"), ZoneOffset.UTC)
        val viewModel = PrayerViewModel(
            permissionChecker = FakePermissionChecker(granted = true),
            locationRepository = FakeLocationRepository(success = true),
            prayerRepository = FakePrayerRepository(),
            clock = afterIsha,
        )
        backgroundScope.launch { viewModel.nextPrayer.collect { } }
        runCurrent()

        val next = viewModel.nextPrayer.value as NextPrayerUi.Ready
        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(today.plusDays(1), next.date)
    }

    // --- Cancellation semantics -----------------------------------------------

    @Test
    fun `a cancelled refresh propagates instead of becoming a failure state`() =
        runTest(dispatcher) {
            val prayers = FakePrayerRepository(cancelled = true)
            val viewModel = viewModel(prayers = prayers, granted = true)
            advanceUntilIdle()

            // `runCatching` used to absorb the CancellationException here and
            // fall through to the no-cache branch, publishing `Failed` for what
            // is only a cancelled load (leaving the screen, clearing the
            // ViewModel). Cancellation must unwind instead.
            assertFalse(
                "Cancellation must not be reported as a failed load",
                viewModel.uiState.value.schedule is PrayerScheduleUi.Failed,
            )
        }

    @Test
    fun `a cancelled tomorrow refresh propagates instead of publishing a null tomorrow`() =
        runTest(dispatcher) {
            val prayers = FakePrayerRepository(tomorrowCancelled = true)
            val viewModel = viewModel(prayers = prayers, granted = true)
            advanceUntilIdle()

            assertFalse(
                "Cancellation of the optional tomorrow load must not be swallowed",
                viewModel.uiState.value.schedule is PrayerScheduleUi.Ready,
            )
            assertFalse(viewModel.uiState.value.schedule is PrayerScheduleUi.Failed)
        }

    @Test
    fun `a plain tomorrow failure still yields a ready schedule without tomorrow`() =
        runTest(dispatcher) {
            // Guards the behaviour the cancellation fix must not change: an
            // ordinary error on the optional tomorrow load is tolerated.
            val prayers = FakePrayerRepository(tomorrow = false)
            val viewModel = viewModel(prayers = prayers, granted = true)
            advanceUntilIdle()

            val ready = viewModel.uiState.value.schedule as PrayerScheduleUi.Ready
            assertEquals(today, ready.today.date)
            assertNull(ready.tomorrow)
            assertFalse(ready.fromCache)
        }

    // --- Helpers -------------------------------------------------------------

    private fun viewModel(
        location: LocationRepository = FakeLocationRepository(success = true),
        prayers: PrayerRepository = FakePrayerRepository(),
        granted: Boolean = true,
        rationale: Boolean = false,
        clock: Clock = this.clock,
    ): PrayerViewModel = PrayerViewModel(
        permissionChecker = FakePermissionChecker(granted = granted, rationale = rationale),
        locationRepository = location,
        prayerRepository = prayers,
        clock = clock,
    )

    private class FakePermissionChecker(
        var granted: Boolean,
        var rationale: Boolean = false,
    ) : LocationPermissionChecker {
        override fun isGranted(): Boolean = granted
        override fun shouldShowRationale(): Boolean = rationale
    }

    private class FakeLocationRepository(
        private val success: Boolean,
        private val label: String? = "Algiers",
    ) : LocationRepository {
        var obtainCount = 0
            private set

        override suspend fun obtainLocation(): Result<GeographicPoint> {
            obtainCount++
            return if (success) {
                Result.success(GeographicPoint(latitude = 36.7538, longitude = 3.0588))
            } else {
                Result.failure(IllegalStateException("no fix"))
            }
        }

        override suspend fun cityLabel(point: GeographicPoint): String? = label
    }

    private class FakeSettingsRepository : SettingsRepository {
        val method = MutableStateFlow(CalculationMethodConfig.defaultMethod)
        val school = MutableStateFlow(AsrSchool.DEFAULT)
        val locationMode = MutableStateFlow(LocationMode.AUTO)
        val manualLatitude = MutableStateFlow<Double?>(null)
        val manualLongitude = MutableStateFlow<Double?>(null)

        override val language: Flow<Language> = MutableStateFlow(Language.ARABIC)
        override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(false)
        override val prayerCalculationMethod: Flow<CalculationMethod> = method
        override val prayerAsrSchool: Flow<AsrSchool> = school
        override val prayerLocationMode: Flow<LocationMode> = locationMode
        override val prayerManualLatitude: Flow<Double?> = manualLatitude
        override val prayerManualLongitude: Flow<Double?> = manualLongitude

        override val notificationsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val prayerRemindersEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val updateNotificationsEnabled: Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setLanguage(language: Language) = Unit
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setOnboardingComplete(completed: Boolean) = Unit
        override suspend fun getDismissedUpdateVersionCode(): Long = 0L
        override suspend fun setDismissedUpdateVersionCode(code: Long) = Unit
        override suspend fun setPrayerCalculationMethod(method: CalculationMethod) {
            this.method.value = method
        }
        override suspend fun setPrayerAsrSchool(school: AsrSchool) {
            this.school.value = school
        }
        override suspend fun setPrayerLocationMode(mode: LocationMode) {
            locationMode.value = mode
        }
        override suspend fun setPrayerManualLocation(latitude: Double, longitude: Double) {
            manualLatitude.value = latitude
            manualLongitude.value = longitude
        }
        override suspend fun clearPrayerManualLocation() {
            manualLatitude.value = null
            manualLongitude.value = null
        }
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setPrayerRemindersEnabled(enabled: Boolean) {}
        override suspend fun setUpdateNotificationsEnabled(enabled: Boolean) {}
    }

    private class FakePrayerRepository(
        private val cached: Boolean = false,
        private val refreshFails: Boolean = false,
        private val tomorrow: Boolean = true,
        private val cancelled: Boolean = false,
        private val tomorrowCancelled: Boolean = false,
        /** Fetch timestamp for today's cached row (null = the cache is empty). */
        private val fetchedAt: Instant? = null,
    ) : PrayerRepository {

        var refreshCount = 0
            private set
        var lastPoint: GeographicPoint? = null
            private set
        var lastMethod: CalculationMethod? = null
            private set
        var lastSchool: Int? = null
            private set

        override suspend fun getCached(
            date: LocalDate,
            point: GeographicPoint,
            method: CalculationMethod,
            school: Int,
        ): DailyPrayerSchedule? = if (cached) schedule(date) else null

        override suspend fun getCachedFetchedAt(
            date: LocalDate,
            point: GeographicPoint,
            method: CalculationMethod,
            school: Int,
        ): Instant? = if (cached && date == today) fetchedAt else null

        override suspend fun refresh(
            date: LocalDate,
            point: GeographicPoint,
            method: CalculationMethod,
            school: Int,
        ): DailyPrayerSchedule {
            refreshCount++
            lastPoint = point
            lastMethod = method
            lastSchool = school
            if (cancelled) throw CancellationException("cancelled")
            // The ViewModel tolerates a missing tomorrow: it only requires today.
            if (refreshFails || (!tomorrow && date > today)) {
                throw PrayerDataException(PrayerErrorKind.NETWORK)
            }
            if (tomorrowCancelled && date > today) throw CancellationException("cancelled")
            return schedule(date)
        }
    }

    /** A [Clock] whose instant a test can advance, to exercise midnight rolls. */
    private class MutableClock(private var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = MutableClock(now)
        fun set(instant: Instant) {
            now = instant
        }
    }

    private companion object {
        val zone: ZoneId = ZoneId.of("Africa/Algiers")
        val today: LocalDate = LocalDate.of(2026, 9, 27)

        /** A complete seven-timings fixture in [zone]; never displayed wording. */
        fun schedule(date: LocalDate): DailyPrayerSchedule = DailyPrayerSchedule(
            date = date,
            hijriReadable = "Fixture",
            timezone = zone.id,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            prayers = listOf(
                PrayerTime(PrayerName.FAJR, LocalTime.of(5, 20)),
                PrayerTime(PrayerName.SUNRISE, LocalTime.of(6, 20)),
                PrayerTime(PrayerName.DHUHR, LocalTime.of(12, 44)),
                PrayerTime(PrayerName.ASR, LocalTime.of(16, 30)),
                PrayerTime(PrayerName.SUNSET, LocalTime.of(19, 40)),
                PrayerTime(PrayerName.MAGHRIB, LocalTime.of(19, 41)),
                PrayerTime(PrayerName.ISHA, LocalTime.of(21, 0)),
            ),
        )
    }
}

