package com.noor.islamic.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.File

/**
 * Covers what a fresh process start observes, which is the contract
 * `SettingsBootstrapper` relies on to restore the language and theme before the
 * first frame.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsBootstrapTest {

    private lateinit var testFile: File
    private lateinit var scope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsDataStore

    @Before
    fun setUp() {
        testFile = File.createTempFile("noor_bootstrap_test", ".preferences_pb")
        scope = TestScope(UnconfinedTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { testFile }
        repository = SettingsDataStore(dataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
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
}
