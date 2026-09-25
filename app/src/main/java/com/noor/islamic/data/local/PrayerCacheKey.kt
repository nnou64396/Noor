package com.noor.islamic.data.local

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.GeographicPoint
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Stable Room primary key for a cached prayer schedule. It identity-binds a
 * gregorian date to a coarse location (2-decimal rounding, ~1 km) and the
 * calculation method, so no location history is ever retained.
 */
object PrayerCacheKey {

    fun of(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): String {
        val lat = BigDecimal.valueOf(point.latitude).setScale(2, RoundingMode.HALF_UP).toPlainString()
        val lng = BigDecimal.valueOf(point.longitude).setScale(2, RoundingMode.HALF_UP).toPlainString()
        return "prayer|$date|$lat|$lng|${method.apiId}"
    }
}