package dz.techvision.noor.data.local.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dz.techvision.noor.domain.sensor.HeadingProvider

/**
 * Device heading via the rotation-vector sensor (a modern fused virtual
 * sensor that already mixes gyroscope/accelerometer/magnetometer). Raw
 * accelerometer values are not used, so the reading stays stable during
 * motion. Emits an exponentially-smoothed azimuth in degrees (0..360).
 */
class HeadingSensor(context: Context) : HeadingProvider {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var accuracyLow = false
    private var hasPrevious = false
    private var previousSmoothed = 0f
    private val smoothingFactor = 0.18f

    private var callback: ((Float, Boolean) -> Unit)? = null
    private var listening = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val raw = ((Math.toDegrees(orientation[0].toDouble()) % 360.0) + 360.0) % 360.0
            val rawDegrees = raw.toFloat()

            val smoothed = if (!hasPrevious) {
                hasPrevious = true
                rawDegrees
            } else {
                val signedDiff = (((rawDegrees - previousSmoothed) % 360f + 540f) % 360f) - 180f
                (((previousSmoothed + smoothingFactor * signedDiff) % 360f) + 360f) % 360f
            }
            previousSmoothed = smoothed

            callback?.invoke(smoothed, accuracyLow)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor != null && sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            accuracyLow = accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
            callback?.invoke(previousSmoothed, accuracyLow)
        }
    }

    override fun isAvailable(): Boolean = rotationSensor != null

    override fun startListening(onHeading: (azimuthDegrees: Float, accuracyLow: Boolean) -> Unit) {
        val sensor = rotationSensor ?: return
        // Registration is driven from a visible-lifecycle observer that can
        // fire more than once (config change, re-entry). Re-registering the
        // same listener would be a no-op in the platform, but guarding here
        // keeps the contract explicit and makes the lifecycle assertable.
        if (listening) {
            callback = onHeading
            return
        }
        callback = onHeading
        if (sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)) {
            listening = true
            hasPrevious = false
        }
    }

    override fun stopListening() {
        callback = null
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(listener)
    }
}
