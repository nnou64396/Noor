package com.noor.islamic.domain.model

/**
 * A coarse geographic position used by prayer-time calculation and the
 * Qibla bearing. Precision is intentionally kept at city level.
 */
data class GeographicPoint(
    val latitude: Double,
    val longitude: Double,
)