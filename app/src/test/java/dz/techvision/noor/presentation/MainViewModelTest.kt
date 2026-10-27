package dz.techvision.noor.presentation

import android.content.Context
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.repository.SettingsRepository
import dz.techvision.noor.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val context: Context = RuntimeEnvironment.getApplication()

    private val info = AppUpdateInfo(
        latestVersion = "1.1.3",
        latestVersionCode = 10100L,
        minimumSupportedVersionCode = 0L,
        sha256 = "expected-sha",
        downloadUrl = "https://github.com/nnou64396/Noor/releases/download/v1.1.3/app-release.apk",
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `get update action invokes the repository download`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val viewModel = MainViewModel(FakeSettingsRepository(), repository)

        viewModel.downloadAndVerifyUpdate(info)

        assertEquals(1, repository.downloadCalls)
        assertEquals(info, repository.lastDownloadInfo)
    }

    @Test
    fun `install action invokes the repository install`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val viewModel = MainViewModel(FakeSettingsRepository(), repository)

        viewModel.installUpdate(context)

        assertEquals(1, repository.installCalls)
    }

    @Test
    fun `retry action invokes the repository retry`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val viewModel = MainViewModel(FakeSettingsRepository(), repository)

        viewModel.retryUpdate(info, context)

        assertEquals(1, repository.retryCalls)
        assertEquals(info, repository.lastRetryInfo)
    }

    @Test
    fun `install permission result delegates and reports whether installation resumed`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository(permissionResult = true)
        val viewModel = MainViewModel(FakeSettingsRepository(), repository)

        assertTrue(viewModel.onInstallPermissionResult(context))
        assertEquals(1, repository.permissionResultCalls)
    }

    @Test
    fun `dismiss update delegates to the repository`() = runTest(dispatcher) {
        val repository = FakeUpdateRepository()
        val viewModel = MainViewModel(FakeSettingsRepository(), repository)

        viewModel.dismissUpdate(10100L)

        assertEquals(10100L, repository.lastDismissedCode)
    }

    private class FakeUpdateRepository(
        private val permissionResult: Boolean = false,
    ) : UpdateRepository {
        override val updateStatus: StateFlow<UpdateStatus> = MutableStateFlow(UpdateStatus.UpToDate)
        override val downloadProgress: StateFlow<Float> = MutableStateFlow(0f)

        var downloadCalls = 0
        var installCalls = 0
        var retryCalls = 0
        var permissionResultCalls = 0
        var lastDownloadInfo: AppUpdateInfo? = null
        var lastRetryInfo: AppUpdateInfo? = null
        var lastDismissedCode: Long? = null

        override suspend fun checkForUpdates(forceCheck: Boolean): UpdateStatus =
            UpdateStatus.UpToDate

        override suspend fun downloadAndVerifyUpdate(info: AppUpdateInfo): Boolean {
            downloadCalls += 1
            lastDownloadInfo = info
            return true
        }

        override fun installUpdate(context: Context) {
            installCalls += 1
        }

        override fun onInstallPermissionResult(context: Context): Boolean {
            permissionResultCalls += 1
            return permissionResult
        }

        override suspend fun retryUpdate(info: AppUpdateInfo, context: Context) {
            retryCalls += 1
            lastRetryInfo = info
        }

        override suspend fun dismissUpdate(versionCode: Long) {
            lastDismissedCode = versionCode
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val language: Flow<Language> = flowOf(Language.ENGLISH)
        override val themeMode: Flow<ThemeMode> = flowOf(ThemeMode.DEFAULT)
        override val onboardingComplete: Flow<Boolean> = flowOf(true)
        override val prayerCalculationMethod: Flow<CalculationMethod> = flowOf(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        override val prayerAsrSchool: Flow<AsrSchool> = flowOf(AsrSchool.DEFAULT)
        override val prayerLocationMode: Flow<LocationMode> = flowOf(LocationMode.DEFAULT)
        override val prayerManualLatitude: Flow<Double?> = flowOf(null)
        override val prayerManualLongitude: Flow<Double?> = flowOf(null)

        override suspend fun setLanguage(language: Language) {}
        override suspend fun setThemeMode(themeMode: ThemeMode) {}
        override suspend fun setOnboardingComplete(completed: Boolean) {}
        override suspend fun setPrayerCalculationMethod(method: CalculationMethod) {}
        override suspend fun setPrayerAsrSchool(school: AsrSchool) {}
        override suspend fun setPrayerLocationMode(mode: LocationMode) {}
        override suspend fun setPrayerManualLocation(latitude: Double, longitude: Double) {}
        override suspend fun clearPrayerManualLocation() {}
        override suspend fun getDismissedUpdateVersionCode(): Long = 0L
        override suspend fun setDismissedUpdateVersionCode(code: Long) {}
    }
}

