package com.noor.islamic.presentation.common

/**
 * Location permission status used by both Prayer and Qibla flows.
 *
 * - GRANTED: enough to fetch a coarse fix.
 * - NOT_REQUESTED: no system dialog has been shown yet.
 * - DENIED_RETRYABLE: a rationale dialog is still useful.
 * - DENIED_PERMANENT: "don't ask again" — the user is directed to Settings.
 */
enum class LocationPermissionUiState {
    GRANTED,
    NOT_REQUESTED,
    DENIED_RETRYABLE,
    DENIED_PERMANENT,
}