package com.noor.islamic.domain.logic

import com.noor.islamic.domain.model.GeographicPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure geographic calculations for the Qibla feature.
 *
 * The Qibla direction is the great-circle initial bearing from the device's
 * coarse location towards the Kaaba, whose coordinates are referenced here
 * as constants (21.422487° N, 39.826206° E).
 */
object QiblaMath {

    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    private val kaaba = GeographicPoint(KAABA_LATITUDE, KAABA_LONGITUDE)

    /** Normalizes any angle into the [0, 360) range (wrap-around safe). */
    fun normalizeDegrees(degrees: Double): Double {
        var result = degrees % 360.0
        if (result < 0) result += 360.0
        return result
    }

    /** Initial (forward) great-circle bearing from `from` towards `to`. */
    fun initialBearing(from: GeographicPoint, to: GeographicPoint): Float {
        val phi1 = Math.toRadians(from.latitude)
        val phi2 = Math.toRadians(to.latitude)
        val dLambda = Math.toRadians(to.longitude - from.longitude)

        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = atan2(y, x)

        return normalizeDegrees(Math.toDegrees(theta)).toFloat()
    }

    /** Compass bearing (0..360) from the device towards the Kaaba. */
    fun bearingToKaaba(from: GeographicPoint): Float = initialBearing(from, kaaba)

    /**
     * The clockwise angle between the device heading and the Qibla bearing.
     * 0 means the device is already pointing at the Qibla.
     */
    fun relativeAngle(headingDegrees: Float, qiblaBearingDegrees: Float): Float =
        normalizeDegrees((qiblaBearingDegrees - headingDegrees).toDouble()).toFloat()
}