package com.noor.islamic.data.local

import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.GeographicPoint
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Stable Room primary key for a cached prayer schedule.
 *
 * The key must identity-bind a gregorian date to a location and a calculation
 * method so the cache can be looked up, but the raw coordinates are never
 * written to disk. The identity string is therefore reduced to a SHA-256
 * digest, which keeps the key stable while removing the readable location.
 *
 * Coordinates are rounded to 2 decimals (~1.1 km) *before* hashing, so the
 * cache still hits for a user who has not moved meaningfully between requests.
 *
 * Note this is pseudonymisation, not anonymisation: the digest is unsalted, so
 * anyone holding the table could confirm a *guessed* coarse location by
 * hashing candidate values. The digest does reveal less than the previous
 * plain-text key, but the table is still treated as location-adjacent and is
 * excluded from backup and device transfer.
 */
object PrayerCacheKey {

    fun of(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): String {
        val lat = BigDecimal.valueOf(point.latitude).setScale(2, RoundingMode.HALF_UP).toPlainString()
        val lng = BigDecimal.valueOf(point.longitude).setScale(2, RoundingMode.HALF_UP).toPlainString()
        val identity = "prayer|$date|$lat|$lng|${method.apiId}"
        return sha256Hex(identity)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val out = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val unsigned = byte.toInt() and 0xFF
            out.append(HEX[unsigned ushr 4])
            out.append(HEX[unsigned and 0x0F])
        }
        return out.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
