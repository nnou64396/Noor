package com.noor.islamic.presentation.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.logic.PrayerScheduleMath
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.repository.LocationPermissionChecker
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.repository.PrayerRepository
import com.noor.islamic.presentation.common.LocationPermissionUiState
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.DateTimeException

/** UI state for the prayer section (shared between Home summary and the Prayer screen). */
data class PrayerUiState(
    val permission: LocationPermissionUiState,
    val isLocating: Boolean = false,
    val locationLabel: String? = null,
    val schedule: PrayerScheduleUi = PrayerScheduleUi.Idle,
    val hasRequestedPermission: Boolean = false,
)

sealed interface PrayerScheduleUi {
    data object Idle : PrayerScheduleUi
    data class Loading(val cached: DailyPrayerSchedule?) : PrayerScheduleUi
    data class Ready(
        val today: DailyPrayerSchedule,
        val tomorrow: DailyPrayerSchedule?,
        val fromCache: Boolean,
    ) : PrayerScheduleUi

    data object Failed : PrayerScheduleUi
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
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PrayerUiState(permission = initialPermission()),
    )
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val point = MutableStateFlow<GeographicPoint?>(null)
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
        if (_uiState.value.permission == LocationPermissionUiState.GRANTED) {
            startLoad()
        }
    }

    /** Re-syncs permission + data when the screen becomes active (also after returning from app settings). */
    fun onScreenEntered() {
        refreshPermissionState()
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
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            locationRepository.obtainLocation()
                .onSuccess { p ->
                    point.value = p
                    val label = locationRepository.cityLabel(p)
                    _uiState.update { it.copy(isLocating = false, locationLabel = label) }
                    loadSchedule(p)
                }
                .onFailure {
                    _uiState.update { it.copy(isLocating = false, locationLabel = null) }
                    if (_uiState.value.schedule !is PrayerScheduleUi.Ready) {
                        _uiState.update { it.copy(schedule = PrayerScheduleUi.Failed) }
                    }
                }
        }
    }

    private suspend fun loadSchedule(p: GeographicPoint) {
        val method = CalculationMethodConfig.defaultMethod
        val today = LocalDate.now(clock)
        val tomorrow = today.plusDays(1)

        val cachedToday = prayerRepository.getCached(today, p, method)
        val cachedTomorrow = prayerRepository.getCached(tomorrow, p, method)
        _uiState.update { it.copy(schedule = PrayerScheduleUi.Loading(cachedToday)) }

        val result = runCatching {
            val freshToday = prayerRepository.refresh(today, p, method)
            val freshTomorrow = kotlin.runCatching { prayerRepository.refresh(tomorrow, p, method) }
                .getOrNull()
            _uiState.update {
                it.copy(schedule = PrayerScheduleUi.Ready(freshToday, freshTomorrow, fromCache = false))
            }
        }
        result.onFailure {
            if (cachedToday != null) {
                _uiState.update {
                    it.copy(schedule = PrayerScheduleUi.Ready(cachedToday, cachedTomorrow, fromCache = true))
                }
            } else {
                _uiState.update { it.copy(schedule = PrayerScheduleUi.Failed) }
            }
        }
    }

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