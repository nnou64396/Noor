package com.noor.islamic.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.LocationMode
import com.noor.islamic.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataStoreTest {

    private lateinit var testFile: File
    private lateinit var testScope: TestScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsDataStore

    @Before
    fun setUp() {
        testFile = File.createTempFile("noor_settings_test", ".preferences_pb")
        testScope = TestScope(UnconfinedTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { testFile }
        repository = SettingsDataStore(dataStore)
    }

    @After
    fun tearDown() {
        testFile.delete()
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
        assertEquals(Language.ENGLISH, repository.language.first())

        repository.setLanguage(Language.FRENCH)
        assertEquals(Language.FRENCH, repository.language.first())
    }

    @Test
    fun persistsThemeMode() = testScope.runTest {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun persistsOnboardingCompletion() = testScope.runTest {
        repository.setOnboardingComplete(true)
        assertEquals(true, repository.onboardingComplete.first())
    }

    @Test
    fun persistsPrayerSettingsAndRestoresThemFromDataStore() = testScope.runTest {
        repository.setPrayerCalculationMethod(CalculationMethod.TURKEY)
        repository.setPrayerAsrSchool(AsrSchool.HANAFI)
        repository.setPrayerLocationMode(LocationMode.MANUAL)
        repository.setPrayerManualLocation(36.7538, 3.0588)
        repository.prayerManualLatitude.first()

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
}