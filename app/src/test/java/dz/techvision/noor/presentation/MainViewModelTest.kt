package dz.techvision.noor.presentation

import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `settingsState emits Ready when settings repository loads`() = runTest(dispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = MainViewModel(settingsRepo)

        val state = viewModel.settingsState.first { it is SettingsUiState.Ready }
        assertTrue(state is SettingsUiState.Ready)
        val ready = state as SettingsUiState.Ready
        assertEquals(Language.ENGLISH, ready.settings.language)
        assertEquals(ThemeMode.DEFAULT, ready.settings.themeMode)
    }

    @Test
    fun `setLanguage updates settings repository`() = runTest(dispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val viewModel = MainViewModel(settingsRepo)

        viewModel.setLanguage(Language.ARABIC)
        assertEquals(Language.ARABIC, settingsRepo.lastSetLanguage)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var lastSetLanguage: Language? = null

        override val language: Flow<Language> = flowOf(Language.ENGLISH)
        override val themeMode: Flow<ThemeMode> = flowOf(ThemeMode.DEFAULT)
        override val onboardingComplete: Flow<Boolean> = flowOf(true)
        override val prayerCalculationMethod: Flow<CalculationMethod> = flowOf(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        override val prayerAsrSchool: Flow<AsrSchool> = flowOf(AsrSchool.DEFAULT)
        override val prayerLocationMode: Flow<LocationMode> = flowOf(LocationMode.DEFAULT)
        override val prayerManualLatitude: Flow<Double?> = flowOf(null)
        override val prayerManualLongitude: Flow<Double?> = flowOf(null)
        override val notificationsEnabled: Flow<Boolean> = flowOf(true)
        override val prayerRemindersEnabled: Flow<Boolean> = flowOf(true)

        override suspend fun setLanguage(language: Language) {
            lastSetLanguage = language
        }
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setOnboardingComplete(completed: Boolean) {}
        override suspend fun setPrayerCalculationMethod(method: CalculationMethod) {}
        override suspend fun setPrayerAsrSchool(school: AsrSchool) {}
        override suspend fun setPrayerLocationMode(mode: LocationMode) {}
        override suspend fun setPrayerManualLocation(latitude: Double, longitude: Double) {}
        override suspend fun clearPrayerManualLocation() {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setPrayerRemindersEnabled(enabled: Boolean) {}
    }
}
