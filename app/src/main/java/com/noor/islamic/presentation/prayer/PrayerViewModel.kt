package com.noor.islamic.presentation.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.logic.PrayerScheduleMath
import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.LocationMode
import com.noor.islamic.domain.model.PrayerDataException
import com.noor.islamic.domain.model.PrayerErrorKind
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.repository.LocationPermissionChecker
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.repository.PrayerRepository
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.presentation.common.LocationPermissionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** UI state for the prayer section (shared between Home summary and the Prayer screen). */
data class PrayerUiState(
    val permission: LocationPermissionUiState,
    val isLocating: Boolean = false,
    val locationLabel: String? = null,
    val schedule: PrayerScheduleUi = PrayerScheduleUi.Idle,
    val hasRequestedPermission: Boolean = false,
    val locationMode: LocationMode = LocationMode.DEFAULT,
)

sealed interface PrayerScheduleUi {
    data object Idle : PrayerScheduleUi
    data class Loading(val cached: DailyPrayerSchedule?) : PrayerScheduleUi
    data class Ready(
        val today: DailyPrayerSchedule,
        val tomorrow: DailyPrayerSchedule?,
        val fromCache: Boolean,
    ) : PrayerScheduleUi

    data class Failed(val kind: PrayerErrorKind) : PrayerScheduleUi
}

sealed interface NextPrayerUi {
    data object Idle : NextPrayerUi
    data class Ready(
        val prayer: PrayerName,
        val date: LocalDate,
        val time: LocalTime,
        val remaining: Duration,
    ) : NextPrayerUi
}

/**
 * Activity-scoped ViewModel shared by Home (summary) and the Prayer screen.
 *
 * Owns permission state, the single coarse location, an offline-first
 * schedule load and a lifecycle-aware per-second countdown (the ticker only
 * runs while a screen is collecting).
 */
class PrayerViewModel(
    private val permissionChecker: LocationPermissionChecker,
    private val locationRepository: LocationRepository,
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PrayerUiState(permission = initialPermission()),
    )
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val point = MutableStateFlow<GeographicPoint?>(null)
    private val selectedMethod = MutableStateFlow<CalculationMethod>(CalculationMethodConfig.defaultMethod)
    private val selectedAsrSchool = MutableStateFlow<AsrSchool>(AsrSchool.DEFAULT)
    private var manualPoint: GeographicPoint? = null
    private var loadJob: Job? = null
    private var permissionRequested = false

    private val scheduleFlow = _uiState.map { it.schedule }

    private val ticker = flow {
        while (true) {
            emit(Instant.now(clock))
            delay(1_000)
        }
    }

    val nextPrayer: StateFlow<NextPrayerUi> =
        combine(scheduleFlow, ticker) { schedule, now -> compute(schedule, now) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = NextPrayerUi.Idle,
            )

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.prayerCalculationMethod.collect { method ->
                    selectedMethod.value = method
                    val currentPoint = point.value
                    if (currentPoint != null) refreshCurrentPoint(currentPoint)
                }
            }
            viewModelScope.launch {
                repo.prayerAsrSchool.collect { school ->
                    selectedAsrSchool.value = school
                    val currentPoint = point.value
                    if (currentPoint != null) refreshCurrentPoint(currentPoint)
                }
            }
            viewModelScope.launch {
                repo.prayerLocationMode
                    .combine(repo.prayerManualLatitude) { mode, latitude -> mode to latitude }
                    .combine(repo.prayerManualLongitude) { (mode, latitude), longitude ->
                        Triple(mode, latitude, longitude)
                    }
                    .collect { (mode, latitude, longitude) ->
                        val candidate = if (
                            latitude != null && longitude != null &&
                            latitude.isFinite() && latitude in -90.0..90.0 &&
                            longitude.isFinite() && longitude in -180.0..180.0
                        ) {
                            GeographicPoint(latitude, longitude)
                        } else {
                            null
                        }
                        onLocationSettingsChanged(mode, candidate)
                    }
            }
        }
        if (_uiState.value.permission == LocationPermissionUiState.GRANTED) {
            startLoad()
        }
    }

    /** Re-syncs permission + data when the screen becomes active (also after returning from app settings). */
    fun onScreenEntered() {
        refreshPermissionState()
        if (_uiState.value.locationMode == LocationMode.MANUAL) {
            loadManualLocation()
        } else {
            reloadIfDayChanged()
        }
    }

    /**
     * If we are already showing a schedule for [LocalDate.now]'s predecessor,
     * reload the current day. Prayer times change at midnight while the app
     * often stays alive, so a fresh entry to the screen must not keep showing
     * yesterday. Unconditionally reloading on every resume would churn the
     * network instead of being served by the once-per-day cache.
     */
    private fun reloadIfDayChanged() {
        if (loadJob?.isActive == true) return
        val p = point.value ?: return
        val loadedDate = when (val s = _uiState.value.schedule) {
            is PrayerScheduleUi.Ready -> s.today.date
            is PrayerScheduleUi.Loading -> s.cached?.date
            else -> return
        } ?: return
        if (loadedDate != LocalDate.now(clock)) {
            loadJob = viewModelScope.launch {
                loadSchedule(p)
            }
        }
    }

    fun onPermissionGranted() {
        permissionRequested = true
        _uiState.update {
            it.copy(permission = LocationPermissionUiState.GRANTED, hasRequestedPermission = true)
        }
        startLoad()
    }

    fun onPermissionDenied() {
        permissionRequested = true
        val permanent = !permissionChecker.shouldShowRationale()
        _uiState.update {
            it.copy(
                permission = if (permanent) LocationPermissionUiState.DENIED_PERMANENT
                else LocationPermissionUiState.DENIED_RETRYABLE,
                hasRequestedPermission = true,
            )
        }
    }

    fun retry() {
        if (_uiState.value.locationMode == LocationMode.MANUAL) {
            loadManualLocation()
            return
        }
        when (uiState.value.permission) {
            LocationPermissionUiState.GRANTED -> startLoad()
            else -> refreshPermissionState()
        }
    }

    private fun initialPermission(): LocationPermissionUiState =
        if (permissionChecker.isGranted()) LocationPermissionUiState.GRANTED
        else LocationPermissionUiState.NOT_REQUESTED

    private fun refreshPermissionState() {
        val granted = permissionChecker.isGranted()
        val current = _uiState.value
        val next = when {
            granted -> LocationPermissionUiState.GRANTED
            permissionRequested && !permissionChecker.shouldShowRationale() ->
                LocationPermissionUiState.DENIED_PERMANENT
            permissionRequested -> LocationPermissionUiState.DENIED_RETRYABLE
            else -> LocationPermissionUiState.NOT_REQUESTED
        }
        _uiState.update { it.copy(permission = next) }
        if (granted && current.permission != LocationPermissionUiState.GRANTED) {
            startLoad()
        }
    }

    private fun startLoad() {
        if (_uiState.value.locationMode == LocationMode.MANUAL) {
            loadManualLocation()
            return
        }
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            locationRepository.obtainLocation()
                .onSuccess { p ->
                    point.value = p
                    _uiState.update { it.copy(isLocating = false) }
                    loadSchedule(p)
                    val label = locationRepository.cityLabel(p)
                    if (label != null) _uiState.update { it.copy(locationLabel = label) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLocating = false, locationLabel = null) }
                    if (_uiState.value.schedule !is PrayerScheduleUi.Ready) {
                        _uiState.update { it.copy(schedule = PrayerScheduleUi.Failed(PrayerErrorKind.LOCATION)) }
                    }
                }
        }
    }

    private fun refreshCurrentPoint(p: GeographicPoint) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadSchedule(p) }
    }

    private fun onLocationSettingsChanged(mode: LocationMode, manual: GeographicPoint?) {
        loadJob?.cancel()
        manualPoint = manual
        _uiState.update { it.copy(locationMode = mode) }
        if (mode == LocationMode.MANUAL) {
            point.value = manual
            _uiState.update { state ->
                state.copy(
                    locationLabel = null,
                    schedule = if (manual == null) {
                        PrayerScheduleUi.Failed(PrayerErrorKind.LOCATION)
                    } else {
                        state.schedule
                    },
                )
            }
            if (manual != null) refreshCurrentPoint(manual)
        } else {
            point.value = null
            if (_uiState.value.permission == LocationPermissionUiState.GRANTED) {
                startLoad()
            } else {
                loadJob?.cancel()
                _uiState.update {
                    it.copy(locationLabel = null, schedule = PrayerScheduleUi.Idle)
                }
            }
        }
    }

    private fun loadManualLocation() {
        val p = manualPoint
        if (p == null) {
            _uiState.update {
                it.copy(schedule = PrayerScheduleUi.Failed(PrayerErrorKind.LOCATION))
            }
            return
        }
        refreshCurrentPoint(p)
    }

    private suspend fun loadSchedule(p: GeographicPoint) {
        val method = selectedMethod.value
        val school = selectedAsrSchool.value.apiId
        val today = LocalDate.now(clock)
        val tomorrow = today.plusDays(1)

        val cachedToday = prayerRepository.getCached(today, p, method, school)
        val cachedTomorrow = prayerRepository.getCached(tomorrow, p, method, school)

        val fetchedAt = prayerRepository.getCachedFetchedAt(today, p, method, school)
        if (cachedToday != null && fetchedAt != null && isFreshFor(cachedToday, fetchedAt)) {
            _uiState.update {
                it.copy(schedule = PrayerScheduleUi.Ready(cachedToday, cachedTomorrow, fromCache = true))
            }
            return
        }

        _uiState.update { it.copy(schedule = PrayerScheduleUi.Loading(cachedToday)) }

        val result = try {
            val freshToday = prayerRepository.refresh(today, p, method, school)
            val freshTomorrow = try {
                prayerRepository.refresh(tomorrow, p, method, school)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _uiState.update {
                it.copy(schedule = PrayerScheduleUi.Ready(freshToday, freshTomorrow, fromCache = false))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        result.onFailure { failure ->
            if (cachedToday != null) {
                _uiState.update {
                    it.copy(schedule = PrayerScheduleUi.Ready(cachedToday, cachedTomorrow, fromCache = true))
                }
            } else {
                _uiState.update { it.copy(schedule = PrayerScheduleUi.Failed(failureKind(failure))) }
            }
        }
    }

    /**
     * True when [fetchedAt] falls on the calendar day [schedule] belongs to,
     * judged in the schedule's own timezone.
     */
    private fun isFreshFor(schedule: DailyPrayerSchedule, fetchedAt: Instant): Boolean {
        val zone = try {
            ZoneId.of(schedule.timezone)
        } catch (_: DateTimeException) {
            clock.zone
        }
        return fetchedAt.atZone(zone).toLocalDate() == schedule.date
    }

    private fun failureKind(e: Throwable): PrayerErrorKind =
        (e as? PrayerDataException)?.kind ?: PrayerErrorKind.UNKNOWN

    private fun compute(schedule: PrayerScheduleUi, now: Instant): NextPrayerUi {
        val today = when (schedule) {
            is PrayerScheduleUi.Ready -> schedule.today
            is PrayerScheduleUi.Loading -> schedule.cached
            else -> null
        } ?: return NextPrayerUi.Idle

        val tomorrow = (schedule as? PrayerScheduleUi.Ready)?.tomorrow
        val zone = try {
            ZoneId.of(today.timezone)
        } catch (_: DateTimeException) {
            ZoneId.systemDefault()
        }
        val zonedNow = ZonedDateTime.ofInstant(now, zone)

        val next = PrayerScheduleMath.nextPrayer(today, tomorrow, zonedNow)
            ?: return NextPrayerUi.Idle
        val remaining = PrayerScheduleMath.countdown(zonedNow, next)
        return NextPrayerUi.Ready(
            prayer = next.prayer,
            date = next.date,
            time = next.time,
            remaining = remaining,
        )
    }
}