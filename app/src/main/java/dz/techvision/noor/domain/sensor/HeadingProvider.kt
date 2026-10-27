package dz.techvision.noor.domain.sensor

/**
 * Reusable abstraction over the device compass, mirroring the
 * `LocationRepository` pattern for Android location services.
 *
 * Implementations must treat [startListening] / [stopListening] as a matched
 * pair that is safe to call repeatedly: starting twice must not register the
 * sensor twice, and stopping when already stopped must be a no-op. Callers are
 * responsible for driving the pair from a visible-lifecycle observer so the
 * sensor is only live while the compass is actually on screen.
 */
interface HeadingProvider {

    /** True when the device exposes a rotation-vector (compass) sensor. */
    fun isAvailable(): Boolean

    /**
     * Starts delivering smoothed azimuth readings in degrees (0..360) together
     * with a flag that is true while the reading accuracy is degraded. Does
     * nothing when no compass sensor is available.
     */
    fun startListening(onHeading: (azimuthDegrees: Float, accuracyLow: Boolean) -> Unit)

    /** Stops delivering readings and releases the sensor. */
    fun stopListening()
}

