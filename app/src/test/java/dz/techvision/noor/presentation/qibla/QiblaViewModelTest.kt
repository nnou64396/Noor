package dz.techvision.noor.presentation.qibla

import androidx.lifecycle.ViewModelStore
import dz.techvision.noor.domain.logic.QiblaMath
import dz.techvision.noor.domain.model.GeographicPoint
import dz.techvision.noor.domain.repository.LocationPermissionChecker
import dz.techvision.noor.domain.repository.LocationRepository
import dz.techvision.noor.domain.sensor.HeadingProvider
import dz.techvision.noor.presentation.common.LocationPermissionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Qibla ViewModel lifecycle over fake collaborators.
 *
 * The focus is the compass: registration must follow the *visible* lifecycle,
 * because the ViewModel is scoped to a saved nav entry and therefore outlives
 * the moment the user leaves the Qibla tab, and it also outlives the Activity
 * being backgrounded.
 */
class QiblaViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // --- Sensor registration -------------------------------------------------

    @Test
    fun `constructing the view model does not start the sensor`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        advanceUntilIdle()

        // A saved nav entry keeps this ViewModel alive after the user taps
        // another tab, so starting the sensor here would leave the compass
        // running for the rest of the process.
        assertEquals(0, sensor.startCount)
        assertFalse(sensor.listening)
    }

    @Test
    fun `the sensor starts when the screen becomes active`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)

        viewModel.onScreenActive()
        advanceUntilIdle()

        assertEquals(1, sensor.startCount)
        assertTrue(sensor.listening)
    }

    @Test
    fun `the sensor stops when the screen becomes inactive`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        viewModel.onScreenActive()
        advanceUntilIdle()

        viewModel.onScreenInactive()
        advanceUntilIdle()

        assertEquals(1, sensor.stopCount)
        assertFalse(sensor.listening)
    }

    @Test
    fun `the sensor does not register twice on repeated activation`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)

        viewModel.onScreenActive()
        viewModel.onScreenActive()
        viewModel.onScreenActive()
        advanceUntilIdle()

        assertEquals(1, sensor.startCount)
        assertTrue(sensor.listening)
    }

    @Test
    fun `deactivation when already inactive does not unregister again`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        viewModel.onScreenActive()
        advanceUntilIdle()

        viewModel.onScreenInactive()
        viewModel.onScreenInactive()
        viewModel.onScreenInactive()
        advanceUntilIdle()

        assertEquals(1, sensor.stopCount)
        assertFalse(sensor.listening)
    }

    @Test
    fun `backgrounding and returning cycles the sensor without leaking a registration`() =
        runTest(dispatcher) {
            val sensor = RecordingHeadingSensor()
            val viewModel = viewModel(sensor = sensor)

            // ON_START -> ON_STOP (Home pressed) -> ON_START -> ON_STOP
            viewModel.onScreenActive()
            viewModel.onScreenInactive()
            viewModel.onScreenActive()
            viewModel.onScreenInactive()
            advanceUntilIdle()

            assertEquals(2, sensor.startCount)
            assertEquals(2, sensor.stopCount)
            assertFalse("Sensor must not stay live in the background", sensor.listening)
        }

    @Test
    fun `clearing the view model releases the sensor as a backstop`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        viewModel.onScreenActive()
        advanceUntilIdle()

        // Go through the real framework clear path rather than a subclass hook.
        ViewModelStore().apply {
            put(KEY, viewModel)
            clear()
        }

        assertEquals(1, sensor.stopCount)
        assertFalse(sensor.listening)
    }

    @Test
    fun `clearing an inactive view model does not unregister again`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        viewModel.onScreenInactive()
        advanceUntilIdle()

        ViewModelStore().apply {
            put(KEY, viewModel)
            clear()
        }

        assertEquals(0, sensor.stopCount)
    }

    // --- Sensor availability -------------------------------------------------

    @Test
    fun `a device without a compass reports unavailable and never registers`() =
        runTest(dispatcher) {
            val sensor = RecordingHeadingSensor(available = false)
            val viewModel = viewModel(sensor = sensor)

            viewModel.onScreenActive()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.sensorAvailable)
            assertEquals(0, sensor.startCount)
            assertFalse(sensor.listening)
        }

    @Test
    fun `a device without a compass still resolves location without crashing`() =
        runTest(dispatcher) {
            val sensor = RecordingHeadingSensor(available = false)
            val viewModel = viewModel(sensor = sensor)
            viewModel.onScreenActive()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.sensorAvailable)
            assertFalse(viewModel.uiState.value.isLocating)
        }

    // --- Heading plumbing ----------------------------------------------------

    @Test
    fun `heading readings reach the ui state while active`() = runTest(dispatcher) {
        val sensor = RecordingHeadingSensor()
        val viewModel = viewModel(sensor = sensor)
        viewModel.onScreenActive()
        advanceUntilIdle()

        sensor.emit(azimuthDegrees = 137.5f, accuracyLow = true)

        assertEquals(137.5f, viewModel.uiState.value.headingDegrees, 0.0001f)
        assertTrue(viewModel.uiState.value.accuracyLow)
    }

    // --- Location / permission ----------------------------------------------

    @Test
    fun `a granted permission resolves the Kaaba bearing on construction`() = runTest(dispatcher) {
        val location = FakeLocationRepository(success = true)
        val viewModel = viewModel(location = location, permissionGranted = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocationPermissionUiState.GRANTED, state.permission)
        assertTrue(state.locationReady)
        assertFalse(state.isLocating)
        assertEquals(1, location.obtainCount)
        assertEquals(expectedBearing, viewModel.uiState.value.bearingToKaaba!!, 0.5f)
    }

    @Test
    fun `a denied permission never requests location`() = runTest(dispatcher) {
        val location = FakeLocationRepository(success = true)
        val viewModel = viewModel(location = location, permissionGranted = false)
        advanceUntilIdle()

        assertEquals(LocationPermissionUiState.NOT_REQUESTED, viewModel.uiState.value.permission)
        assertEquals(0, location.obtainCount)
        assertFalse(viewModel.uiState.value.locationReady)
    }

    @Test
    fun `an unavailable location ends the loading state instead of spinning`() =
        runTest(dispatcher) {
            val location = FakeLocationRepository(success = false)
            val viewModel = viewModel(location = location, permissionGranted = true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse("A failed fix must not leave the UI locating", state.isLocating)
            assertFalse(state.locationReady)
            assertNull(state.bearingToKaaba)
        }

    @Test
    fun `a permission revoked from settings is picked up on the next resume`() =
        runTest(dispatcher) {
            val checker = FakePermissionChecker(granted = true)
            val sensor = RecordingHeadingSensor()
            val location = FakeLocationRepository(success = true)
            val viewModel = QiblaViewModel(checker, location, sensor)
            advanceUntilIdle()

            assertEquals(LocationPermissionUiState.GRANTED, viewModel.uiState.value.permission)

            // The user revokes access from the system settings while the app
            // stays alive. The permission was granted up front (no dialog), so
            // the check falls back to the "not requested" state.
            checker.granted = false
            viewModel.onScreenEntered()
            advanceUntilIdle()

            assertEquals(LocationPermissionUiState.NOT_REQUESTED, viewModel.uiState.value.permission)
        }

    @Test
    fun `a dialog-granted permission revoked later becomes permanently denied and stops the sensor`() =
        runTest(dispatcher) {
            val checker = FakePermissionChecker(granted = true)
            val sensor = RecordingHeadingSensor()
            val viewModel = QiblaViewModel(checker, FakeLocationRepository(success = true), sensor)
            viewModel.onPermissionGranted()
            viewModel.onScreenActive()
            advanceUntilIdle()
            assertTrue(sensor.listening)

            // Revoked, rationales are no longer shown -> permanent denial.
            checker.granted = false
            viewModel.onScreenEntered()
            advanceUntilIdle()

            assertEquals(
                LocationPermissionUiState.DENIED_PERMANENT,
                viewModel.uiState.value.permission,
            )
            assertFalse("A revoked permission must stop the sensor", sensor.listening)
        }

    @Test
    fun `re-entering an already resolved screen does not fetch the location again`() =
        runTest(dispatcher) {
            val sensor = RecordingHeadingSensor()
            val location = FakeLocationRepository(success = true)
            val viewModel = viewModel(sensor = sensor, location = location, permissionGranted = true)
            advanceUntilIdle()

            // Leave the tab, come back, and the compass resumes — but the fix
            // that was already resolved must not be requested a second time.
            viewModel.onScreenActive()
            viewModel.onScreenInactive()
            viewModel.onScreenEntered()
            viewModel.onScreenActive()
            advanceUntilIdle()

            assertEquals(1, location.obtainCount)
            assertEquals(2, sensor.startCount)
            assertTrue(sensor.listening)
        }

    // --- Location request concurrency -----------------------------------------

    @Test
    fun `a resume while the first fix is in flight does not request the location twice`() =
        runTest(dispatcher) {
            val location = ControllableLocationRepository()
            val gate = CompletableDeferred<Unit>()
            location.gate = gate
            // The first fix fails, so `locationReady` stays false and a later
            // re-entry is legitimately allowed to try again.
            location.enqueue(Result.failure(IllegalStateException("no fix")))

            val viewModel = viewModel(location = location, permissionGranted = true)

            // `init` started the first request, which is now suspended inside
            // the repository, so the bearing is not resolved yet.
            assertEquals(1, location.obtainCount)
            assertTrue(viewModel.uiState.value.isLocating)

            // The ON_RESUME catch-up on the same cold start must not open a
            // second overlapping fix for the same location.
            viewModel.onScreenEntered()
            assertEquals(1, location.obtainCount)

            // The first request finishes and fails.
            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.locationReady)
            assertFalse(viewModel.uiState.value.isLocating)

            // The guard is not a permanent busy state: once the request is no
            // longer in flight, a later re-entry resolves the location again.
            location.gate = null
            location.enqueue(Result.success(point))
            viewModel.onScreenEntered()
            advanceUntilIdle()

            assertEquals(2, location.obtainCount)
            assertTrue(viewModel.uiState.value.locationReady)
            assertEquals(expectedBearing, viewModel.uiState.value.bearingToKaaba)
        }

    // --- Helpers -------------------------------------------------------------

    private fun viewModel(
        sensor: RecordingHeadingSensor = RecordingHeadingSensor(),
        location: LocationRepository = FakeLocationRepository(success = true),
        permissionGranted: Boolean = true,
    ) = QiblaViewModel(
        permissionChecker = FakePermissionChecker(granted = permissionGranted),
        locationRepository = location,
        headingSensor = sensor,
    )

    private class FakePermissionChecker(
        var granted: Boolean,
        private val rationale: Boolean = false,
    ) : LocationPermissionChecker {
        override fun isGranted(): Boolean = granted
        override fun shouldShowRationale(): Boolean = rationale
    }

    private class FakeLocationRepository(
        private val success: Boolean,
    ) : LocationRepository {
        var obtainCount = 0
            private set

        override suspend fun obtainLocation(): Result<GeographicPoint> {
            obtainCount++
            return if (success) Result.success(point) else Result.failure(IllegalStateException("no fix"))
        }

        override suspend fun cityLabel(point: GeographicPoint): String? = "Algiers"
    }

    /**
     * A repository whose fix can be held open with [gate], so a test can observe
     * the window in which a request is genuinely in flight, and whose result can
     * be scripted per call with [enqueue].
     */
    private class ControllableLocationRepository : LocationRepository {
        var obtainCount = 0
            private set

        var gate: CompletableDeferred<Unit>? = null

        private val results = ArrayDeque<Result<GeographicPoint>>()

        fun enqueue(result: Result<GeographicPoint>) {
            results.addLast(result)
        }

        override suspend fun obtainLocation(): Result<GeographicPoint> {
            obtainCount++
            gate?.await()
            return if (results.isEmpty()) Result.success(point) else results.removeFirst()
        }

        override suspend fun cityLabel(point: GeographicPoint): String? = "Algiers"
    }

    private class RecordingHeadingSensor(
        private val available: Boolean = true,
    ) : HeadingProvider {
        var startCount = 0
            private set
        var stopCount = 0
            private set
        var listening = false
            private set
        private var callback: ((Float, Boolean) -> Unit)? = null

        override fun isAvailable(): Boolean = available

        override fun startListening(onHeading: (Float, Boolean) -> Unit) {
            if (!available) return
            startCount++
            listening = true
            callback = onHeading
        }

        override fun stopListening() {
            if (!listening) return
            stopCount++
            listening = false
            callback = null
        }

        fun emit(azimuthDegrees: Float, accuracyLow: Boolean) {
            callback?.invoke(azimuthDegrees, accuracyLow)
        }
    }

    private companion object {
        const val KEY = "qibla"

        val point = GeographicPoint(latitude = 36.7538, longitude = 3.0588)

        /** QiblaMath's own bearing for [point], recomputed so the expectation is not copied. */
        val expectedBearing = QiblaMath.bearingToKaaba(point)
    }
}

