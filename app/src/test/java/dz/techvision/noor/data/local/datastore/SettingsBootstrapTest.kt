package dz.techvision.noor.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.usecase.ObserveSettingsUseCase
import dz.techvision.noor.utils.SettingsBootstrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.io.IOException
import java.nio.file.Files

/**
 * Covers what a fresh process start observes, which is the contract
 * `SettingsBootstrapper` relies on to restore the language and theme before the
 * first frame.
 *
 * Runs under Robolectric for the same reason as [SettingsDataStoreTest]: a plain
 * JVM unit test reports `Build.VERSION.SDK_INT == 0`, which sends DataStore down
 * its pre-API-26 `File.renameTo` path. That path cannot replace an existing file
 * on Windows, while every supported Android version uses `Files.move`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsBootstrapTest {

    private lateinit var testDir: File
    private lateinit var testFile: File
    private lateinit var scope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsDataStore

    @Before
    fun setUp() {
        testDir = Files.createTempDirectory("noor_bootstrap_test").toFile()
        testFile = File(testDir, "noor_settings.preferences_pb")
        // Eager dispatch is required here: the class under test reads settings
        // through a deliberately blocking `runBlocking`, so the DataStore work it
        // awaits has to run on the calling thread instead of a queued dispatcher.
        scope = TestScope(UnconfinedTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
        repository = SettingsDataStore(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        testDir.deleteRecursively()
    }

    /**
     * Exactly what `SettingsBootstrapper.readPersistedSettings()` performs at
     * process start: take the first emission of the settings stream.
     */
    private suspend fun settingsAsSeenAtStartup(): AppSettings =
        ObserveSettingsUseCase(repository).invoke().first()

    @Test
    fun `a first-time user starts on the existing defaults`() = scope.runTest {
        val settings = settingsAsSeenAtStartup()

        assertEquals(Language.DEFAULT, settings.language)
        assertEquals(ThemeMode.DEFAULT, settings.themeMode)
        assertFalse(settings.onboardingComplete)
    }

    @Test
    fun `a stored language is what a restart observes`() = scope.runTest {
        repository.setLanguage(Language.FRENCH)

        assertEquals(Language.FRENCH, settingsAsSeenAtStartup().language)
    }

    @Test
    fun `a stored theme is what a restart observes`() = scope.runTest {
        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settingsAsSeenAtStartup().themeMode)
    }

    @Test
    fun `language and theme are restored together`() = scope.runTest {
        repository.setLanguage(Language.ENGLISH)
        repository.setThemeMode(ThemeMode.LIGHT)

        val restored = settingsAsSeenAtStartup()
        assertEquals(Language.ENGLISH, restored.language)
        assertEquals(ThemeMode.LIGHT, restored.themeMode)
    }

    @Test
    fun `onboarding completion is what a restart observes`() = scope.runTest {
        repository.setOnboardingComplete(true)

        assertTrue(settingsAsSeenAtStartup().onboardingComplete)
    }

    @Test
    fun `the last chosen language wins`() = scope.runTest {
        repository.setLanguage(Language.ENGLISH)
        repository.setLanguage(Language.ARABIC)

        assertEquals(Language.ARABIC, settingsAsSeenAtStartup().language)
    }

    @Test
    fun `every supported language round-trips through storage`() = scope.runTest {
        Language.entries.forEach { language ->
            repository.setLanguage(language)
            assertEquals(language, Language.fromCode(language.code))
            assertEquals(language, settingsAsSeenAtStartup().language)
        }
    }

    @Test
    fun `every theme mode round-trips through storage`() = scope.runTest {
        ThemeMode.entries.forEach { mode ->
            repository.setThemeMode(mode)
            assertEquals(mode, settingsAsSeenAtStartup().themeMode)
        }
    }

    @Test
    fun `an unrecognised stored code falls back to the default`() {
        assertEquals(Language.DEFAULT, Language.fromCode("de"))
        assertEquals(Language.DEFAULT, Language.fromCode(null))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromStorageKey("sepia"))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromStorageKey(null))
    }

    @Test
    fun `the settings stream emits again when a value changes`() = scope.runTest {
        assertEquals(ThemeMode.SYSTEM, settingsAsSeenAtStartup().themeMode)

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settingsAsSeenAtStartup().themeMode)
    }

    // --- File lifecycle regression --------------------------------------------

    @Test
    fun `a write creates the store file that a later process start reads`() = scope.runTest {
        assertFalse(testFile.exists())

        repository.setLanguage(Language.FRENCH)
        repository.setThemeMode(ThemeMode.DARK)

        assertTrue(testFile.isFile)
        assertTrue(testFile.length() > 0L)
        assertFalse(File(testFile.absolutePath + ".tmp").exists())
        assertEquals(Language.FRENCH, settingsAsSeenAtStartup().language)
        assertEquals(ThemeMode.DARK, settingsAsSeenAtStartup().themeMode)
    }

    // --- Bounded startup read --------------------------------------------------

    @Test
    fun `a healthy bounded read returns exactly what a restart observes`() {
        val stored = AppSettings(
            language = Language.FRENCH,
            themeMode = ThemeMode.DARK,
            onboardingComplete = true,
        )

        val restored = SettingsBootstrapper.readBoundedSettings(timeoutMillis = 5_000) { stored }

        assertEquals(stored, restored)
    }

    @Test
    fun `a real store read inside the bound is unchanged`() = scope.runTest {
        repository.setLanguage(Language.ENGLISH)
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setOnboardingComplete(true)

        val restored = SettingsBootstrapper.readBoundedSettings(timeoutMillis = 5_000) {
            ObserveSettingsUseCase(repository).invoke().first()
        }

        assertEquals(Language.ENGLISH, restored.language)
        assertEquals(ThemeMode.LIGHT, restored.themeMode)
        assertTrue(restored.onboardingComplete)
    }

    @Test
    fun `a read that never answers falls back to the first run defaults`() {
        val restored = SettingsBootstrapper.readBoundedSettings(timeoutMillis = 50) {
            // Simulates a wedged or slow preferences file: the emission below
            // never arrives, so only the bound can end the wait.
            kotlinx.coroutines.awaitCancellation()
            error("unreachable")
        }

        assertEquals(AppSettings(), restored)
        assertEquals(Language.DEFAULT, restored.language)
        assertEquals(ThemeMode.DEFAULT, restored.themeMode)
    }

    @Test
    fun `an unreadable store falls back to the first run defaults`() {
        val restored = SettingsBootstrapper.readBoundedSettings(timeoutMillis = 5_000) {
            throw IOException("preferences file is corrupt")
        }

        assertEquals(AppSettings(), restored)
        assertEquals(ThemeMode.DEFAULT, restored.themeMode)
    }
}

