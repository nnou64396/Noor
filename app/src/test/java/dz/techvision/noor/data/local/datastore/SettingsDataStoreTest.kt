package dz.techvision.noor.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Exercises the real, file-backed DataStore rather than a fake.
 *
 * The tests run under Robolectric so `Build.VERSION.SDK_INT` reports a real
 * Android API level. Under a plain JVM unit test it is `0`, which makes DataStore
 * fall back to `File.renameTo`; that call cannot replace an existing file on
 * Windows, so every write after the first one would fail even though every
 * supported Android version (`minSdk = 26`) uses the replacing `Files.move` path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsDataStoreTest {

    private lateinit var testDir: File
    private lateinit var testFile: File
    private lateinit var testScope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsDataStore

    @Before
    fun setUp() {
        // An isolated directory plus a path that does not exist yet: the state of
        // a fresh install, with no dependency on previously written app data or
        // on anything already present in the machine's temporary directory.
        testDir = Files.createTempDirectory("noor_settings_test").toFile()
        testFile = File(testDir, "noor_settings.preferences_pb")
        testScope = TestScope(StandardTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { testFile }
        repository = SettingsDataStore(dataStore)
    }

    @After
    fun tearDown() {
        // Cancelling the scope closes DataStore's file connection, which also
        // releases the file so the directory can be removed reliably.
        testScope.cancel()
        testDir.deleteRecursively()
    }

    @Test
    fun defaultsAreUsedWhenNothingIsStored() = testScope.runTest {
        assertEquals(Language.ARABIC, repository.language.first())
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals(false, repository.onboardingComplete.first())
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, repository.prayerCalculationMethod.first())
        assertEquals(AsrSchool.STANDARD, repository.prayerAsrSchool.first())
        assertEquals(LocationMode.AUTO, repository.prayerLocationMode.first())
        assertEquals(null, repository.prayerManualLatitude.first())
        assertEquals(null, repository.prayerManualLongitude.first())
    }

    @Test
    fun persistsLanguage() = testScope.runTest {
        repository.setLanguage(Language.ENGLISH)
        advanceUntilIdle()
        assertEquals(Language.ENGLISH, repository.language.first())

        repository.setLanguage(Language.FRENCH)
        advanceUntilIdle()
        assertEquals(Language.FRENCH, repository.language.first())
    }

    @Test
    fun persistsThemeMode() = testScope.runTest {
        repository.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun persistsOnboardingCompletion() = testScope.runTest {
        repository.setOnboardingComplete(true)
        advanceUntilIdle()
        assertEquals(true, repository.onboardingComplete.first())
    }

    @Test
    fun persistsPrayerSettingsAndRestoresThemFromDataStore() = testScope.runTest {
        repository.setPrayerCalculationMethod(CalculationMethod.TURKEY)
        repository.setPrayerAsrSchool(AsrSchool.HANAFI)
        repository.setPrayerLocationMode(LocationMode.MANUAL)
        repository.setPrayerManualLocation(36.7538, 3.0588)
        advanceUntilIdle()

        val restored = SettingsDataStore(dataStore)
        assertEquals(CalculationMethod.TURKEY, restored.prayerCalculationMethod.first())
        assertEquals(AsrSchool.HANAFI, restored.prayerAsrSchool.first())
        assertEquals(LocationMode.MANUAL, restored.prayerLocationMode.first())
        assertEquals(36.7538, restored.prayerManualLatitude.first()!!, 0.0)
        assertEquals(3.0588, restored.prayerManualLongitude.first()!!, 0.0)
    }

    @Test
    fun rejectsInvalidManualCoordinatesWithoutReplacingStoredCoordinates() = testScope.runTest {
        repository.setPrayerManualLocation(36.75, 3.05)
        advanceUntilIdle()

        val invalidLatitude = runCatching {
            repository.setPrayerManualLocation(90.1, 3.05)
        }.exceptionOrNull()
        val invalidLongitude = runCatching {
            repository.setPrayerManualLocation(36.75, Double.NaN)
        }.exceptionOrNull()

        assertEquals(IllegalArgumentException::class.java, invalidLatitude?.javaClass)
        assertEquals(IllegalArgumentException::class.java, invalidLongitude?.javaClass)
        assertEquals(36.75, repository.prayerManualLatitude.first()!!, 0.0)
        assertEquals(3.05, repository.prayerManualLongitude.first()!!, 0.0)
    }

    // --- File lifecycle regressions -------------------------------------------

    @Test
    fun firstWriteCreatesTheStoreFileAndLeavesNoScratchFileBehind() = testScope.runTest {
        assertFalse(testFile.exists())

        repository.setLanguage(Language.ENGLISH)
        advanceUntilIdle()

        assertTrue(testFile.isFile)
        assertTrue(testFile.length() > 0L)
        assertFalse(File(testFile.absolutePath + ".tmp").exists())
        assertEquals(Language.ENGLISH, repository.language.first())
    }

    @Test
    fun writesReplaceAnExistingStoreFileRepeatedly() = testScope.runTest {
        // DataStore writes to `<file>.tmp` and then replaces the store. The
        // second write is the regression case: it has to replace a store file
        // that the first write created.
        repository.setLanguage(Language.ENGLISH)
        advanceUntilIdle()
        repository.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        repository.setOnboardingComplete(true)
        advanceUntilIdle()

        assertEquals(Language.ENGLISH, repository.language.first())
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
        assertTrue(repository.onboardingComplete.first())
        assertTrue(testFile.isFile)
        assertFalse(File(testFile.absolutePath + ".tmp").exists())
    }

    @Test
    fun aFreshStoreInstanceReadsAndUpdatesAFileWrittenByThePreviousOne() = runBlocking {
        val restartFile = File(testDir, "noor_settings_restart.preferences_pb")

        // Real dispatchers here: this test models two separate process
        // lifetimes, so each DataStore gets its own scope and is fully disposed
        // before the next one opens the same file.
        val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val firstStore = PreferenceDataStoreFactory.create(scope = firstScope) { restartFile }
        try {
            SettingsDataStore(firstStore).setLanguage(Language.FRENCH)
        } finally {
            firstScope.coroutineContext.job.cancelAndJoin()
        }

        val secondScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val secondStore = PreferenceDataStoreFactory.create(scope = secondScope) { restartFile }
        try {
            val restarted = SettingsDataStore(secondStore)
            assertEquals(Language.FRENCH, restarted.language.first())

            restarted.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, restarted.themeMode.first())
        } finally {
            secondScope.coroutineContext.job.cancelAndJoin()
        }
    }
}

