package dz.techvision.noor.data.local

import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.CalculationMethodConfig
import dz.techvision.noor.domain.model.GeographicPoint
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Stable Room primary key for a cached prayer schedule.
 *
 * The key identity-binds a Gregorian date, request location, calculation method,
 * and Asr school so the cache cannot be incorrectly reused across materially
 * different request inputs. Raw coordinates are never persisted in cleartext.
 */
object PrayerCacheKey {

    fun of(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
        school: Int = CalculationMethodConfig.defaultSchool,
    ): String {
        val lat = BigDecimal.valueOf(point.latitude).stripTrailingZeros().toPlainString()
        val lng = BigDecimal.valueOf(point.longitude).stripTrailingZeros().toPlainString()
        val identity = "prayer|$date|$lat|$lng|${method.apiId}|$school"
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

