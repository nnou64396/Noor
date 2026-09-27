package com.noor.islamic.presentation.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.logic.QiblaMath
import com.noor.islamic.domain.repository.LocationPermissionChecker
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.sensor.HeadingProvider
import com.noor.islamic.presentation.common.LocationPermissionUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QiblaUiState(
    val permission: LocationPermissionUiState,
    val headingDegrees: Float = 0f,
    val bearingToKaaba: Float? = null,
    val locationReady: Boolean = false,
    val isLocating: Boolean = false,
    val sensorAvailable: Boolean = true,
    val accuracyLow: Boolean = false,
    val hasRequestedPermission: Boolean = false,
)

/**
 * Drives the Qibla compass: reuses the same location/permission layer as
 * Prayer and computes the bearing towards the Kaaba from the coarse fix.
 *
 * The rotation-vector sensor is *not* started from [init]. The ViewModel is
 * scoped to a saved nav entry, so it outlives the moment the user taps away
 * from the Qibla tab, and it also outlives the Activity going to the
 * background. Registration is therefore driven explicitly by the screen
 * through [onScreenActive] / [onScreenInactive], which are wired to the
 * visible lifecycle; [onCleared] is only a backstop.
 */
class QiblaViewModel(
    private val permissionChecker: LocationPermissionChecker,
    private val locationRepository: LocationRepository,
    private val headingSensor: HeadingProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        QiblaUiState(
            permission = if (permissionChecker.isGranted()) {
                LocationPermissionUiState.GRANTED
            } else {
                LocationPermissionUiState.NOT_REQUESTED
            },
            sensorAvailable = headingSensor.isAvailable(),
        ),
    )
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var permissionRequested = false
    private var sensorActive = false

    init {
        refreshPermissionState()
        if (_uiState.value.permission == LocationPermissionUiState.GRANTED) {
            resolveLocation()
        }
    }

    override fun onCleared() {
        sensorActive = false
        headingSensor.stopListening()
    }

    /** Called when the Qibla screen becomes visible and the Activity is resumed. */
    fun onScreenActive() {
        startSensor()
    }

    /** Called when the Qibla screen is no longer visible, or the Activity is backgrounded. */
    fun onScreenInactive() {
        stopSensor()
    }

    fun onScreenEntered() {
        refreshPermissionState()
        if (uiState.value.permission == LocationPermissionUiState.GRANTED && !uiState.value.locationReady) {
            resolveLocation()
        }
    }

    fun onPermissionGranted() {
        permissionRequested = true
        _uiState.update {
            it.copy(permission = LocationPermissionUiState.GRANTED, hasRequestedPermission = true)
        }
        resolveLocation()
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
            LocationPermissionUiState.GRANTED -> resolveLocation()
            else -> refreshPermissionState()
        }
    }

    private fun refreshPermissionState() {
        if (!permissionChecker.isGranted()) return
        _uiState.update {
            it.copy(permission = LocationPermissionUiState.GRANTED, hasRequestedPermission = permissionRequested)
        }
    }

    private fun resolveLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            locationRepository.obtainLocation()
                .onSuccess { point ->
                    val bearing = QiblaMath.bearingToKaaba(point)
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationReady = true,
                            bearingToKaaba = bearing,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLocating = false, locationReady = false, bearingToKaaba = null) }
                }
        }
    }

    private fun startSensor() {
        if (sensorActive) return
        if (!headingSensor.isAvailable()) return
        sensorActive = true
        headingSensor.startListening(onHeading = ::onHeading)
    }

    private fun stopSensor() {
        if (!sensorActive) return
        sensorActive = false
        headingSensor.stopListening()
    }

    private fun onHeading(azimuthDegrees: Float, accuracyLow: Boolean) {
        _uiState.update { it.copy(headingDegrees = azimuthDegrees, accuracyLow = accuracyLow) }
    }
}