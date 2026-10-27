package dz.techvision.noor.domain.logic

import dz.techvision.noor.domain.model.GeographicPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaMathTest {

    private fun assertApprox(expected: Double, actual: Float, tolerance: Double = 5.0) {
        assertTrue(
            "Expected ~$expected but got $actual",
            kotlin.math.abs(expected - actual) <= tolerance,
        )
    }

    @Test
    fun `normalize keeps zero`() {
        assertEquals(0.0, QiblaMath.normalizeDegrees(0.0), 1e-9)
    }

    @Test
    fun `normalize wraps 360 back to zero`() {
        assertEquals(0.0, QiblaMath.normalizeDegrees(360.0), 1e-9)
    }

    @Test
    fun `normalize wraps negative towards 360`() {
        assertEquals(350.0, QiblaMath.normalizeDegrees(-10.0), 1e-9)
    }

    @Test
    fun `normalize wraps large angles`() {
        assertEquals(40.0, QiblaMath.normalizeDegrees(400.0), 1e-9)
        assertEquals(359.0, QiblaMath.normalizeDegrees(719.0), 1e-9)
    }

    @Test
    fun `bearing from the same longitude east of the kaaba points east`() {
        val eastOfKaaba = GeographicPoint(QiblaMath.KAABA_LATITUDE, QiblaMath.KAABA_LONGITUDE + 0.2)
        val bearing = QiblaMath.bearingToKaaba(eastOfKaaba)
        assertApprox(270.0, bearing, tolerance = 5.0)
    }

    @Test
    fun `bearing from algiers is within the expected range`() {
        val algiers = GeographicPoint(36.7538, 3.0588)
        val bearing = QiblaMath.bearingToKaaba(algiers)
        assertTrue("Algiers bearing was $bearing", bearing in 100f..110f)
    }

    @Test
    fun `bearing from new york is within the expected range`() {
        val newYork = GeographicPoint(40.7128, -74.0060)
        val bearing = QiblaMath.bearingToKaaba(newYork)
        assertTrue("New York bearing was $bearing", bearing in 55f..65f)
    }

    @Test
    fun `bearing from the kaaba itself is close to zero`() {
        val bearing = QiblaMath.bearingToKaaba(
            GeographicPoint(QiblaMath.KAABA_LATITUDE, QiblaMath.KAABA_LONGITUDE),
        )
        assertTrue("Self bearing was $bearing", bearing < 45f)
    }

    @Test
    fun `relative angle is zero when pointing at the kaaba`() {
        assertEquals(0f, QiblaMath.relativeAngle(105f, 105f), 1e-3f)
    }

    @Test
    fun `relative angle is bearing minus heading`() {
        assertEquals(90f, QiblaMath.relativeAngle(0f, 90f), 1e-3f)
        assertEquals(0f, QiblaMath.relativeAngle(90f, 90f), 1e-3f)
    }

    @Test
    fun `relative angle wraps correctly near 0 slash 360 boundary`() {
        assertEquals(20f, QiblaMath.relativeAngle(350f, 10f), 1e-3f)
        assertEquals(340f, QiblaMath.relativeAngle(30f, 10f), 1e-3f)
    }
}
