package com.noor.islamic.data.repository

import com.noor.islamic.data.remote.update.UpdateApi
import com.noor.islamic.data.remote.update.UpdateManifestDto
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class UpdateRepositoryTest {

    private class FakeSettingsRepository : SettingsRepository {
        private var dismissedCode = 0L

        override suspend fun getDismissedUpdateVersionCode(): Long = dismissedCode

        override suspend fun setDismissedUpdateVersionCode(code: Long) {
            dismissedCode = code
        }

        // Stubs for other settings methods
        override val language = flowOf(com.noor.islamic.domain.model.Language.ENGLISH)
        override val themeMode = flowOf(com.noor.islamic.domain.model.ThemeMode.DEFAULT)
        override val onboardingComplete = flowOf(true)
        override val prayerCalculationMethod = flowOf(com.noor.islamic.domain.model.CalculationMethod.MUSLIM_WORLD_LEAGUE)
        override val prayerAsrSchool = flowOf(com.noor.islamic.domain.model.AsrSchool.DEFAULT)
        override val prayerLocationMode = flowOf(com.noor.islamic.domain.model.LocationMode.DEFAULT)
        override val prayerManualLatitude = flowOf(null)
        override val prayerManualLongitude = flowOf(null)

        override suspend fun setLanguage(language: com.noor.islamic.domain.model.Language) {}
        override suspend fun setThemeMode(themeMode: com.noor.islamic.domain.model.ThemeMode) {}
        override suspend fun setOnboardingComplete(completed: Boolean) {}
        override suspend fun setPrayerCalculationMethod(method: com.noor.islamic.domain.model.CalculationMethod) {}
        override suspend fun setPrayerAsrSchool(school: com.noor.islamic.domain.model.AsrSchool) {}
        override suspend fun setPrayerLocationMode(mode: com.noor.islamic.domain.model.LocationMode) {}
        override suspend fun setPrayerManualLocation(latitude: Double, longitude: Double) {}
        override suspend fun clearPrayerManualLocation() {}
    }

    private class FakeUpdateApi(
        private val response: Response<UpdateManifestDto>?,
        private val shouldThrow: Boolean = false,
    ) : UpdateApi {
        override suspend fun fetchManifest(manifestUrl: String): Response<UpdateManifestDto> {
            if (shouldThrow) throw java.io.IOException("Network failure")
            return response ?: Response.error(404, "".toResponseBody(null))
        }
    }

    @Test
    fun `when manifest url is null returns Disabled`() = runTest {
        val settings = FakeSettingsRepository()
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { null },
            apiProvider = { FakeUpdateApi(null) },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.Disabled, status)
    }

    @Test
    fun `when current version is up to date returns UpToDate`() = runTest {
        val settings = FakeSettingsRepository()
        val manifest = UpdateManifestDto(
            latestVersion = "1.0.0",
            latestVersionCode = 1L,
            minimumSupportedVersionCode = 1L,
        )
        val api = FakeUpdateApi(Response.success(manifest))
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { "https://example.com/manifest.json" },
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.UpToDate, status)
    }

    @Test
    fun `when newer version available returns UpdateAvailable`() = runTest {
        val settings = FakeSettingsRepository()
        val manifest = UpdateManifestDto(
            latestVersion = "2.0.0",
            latestVersionCode = 2L,
            minimumSupportedVersionCode = 1L,
        )
        val api = FakeUpdateApi(Response.success(manifest))
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { "https://example.com/manifest.json" },
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assert(status is UpdateStatus.UpdateAvailable)
        assertEquals(2L, (status as UpdateStatus.UpdateAvailable).info.latestVersionCode)
    }

    @Test
    fun `when current version below minimum supported returns UpdateRequired`() = runTest {
        val settings = FakeSettingsRepository()
        val manifest = UpdateManifestDto(
            latestVersion = "2.0.0",
            latestVersionCode = 2L,
            minimumSupportedVersionCode = 2L,
        )
        val api = FakeUpdateApi(Response.success(manifest))
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { "https://example.com/manifest.json" },
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assert(status is UpdateStatus.UpdateRequired)
    }

    @Test
    fun `when update is dismissed returns UpToDate unless forced`() = runTest {
        val settings = FakeSettingsRepository()
        settings.setDismissedUpdateVersionCode(2L)
        val manifest = UpdateManifestDto(
            latestVersion = "2.0.0",
            latestVersionCode = 2L,
            minimumSupportedVersionCode = 1L,
        )
        val api = FakeUpdateApi(Response.success(manifest))
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { "https://example.com/manifest.json" },
            apiProvider = { api },
        )

        val statusNormal = repo.checkForUpdates(forceCheck = false)
        assertEquals(UpdateStatus.UpToDate, statusNormal)

        val statusForced = repo.checkForUpdates(forceCheck = true)
        assert(statusForced is UpdateStatus.UpdateAvailable)
    }

    @Test
    fun `when network throws exception returns CheckUnavailable safely`() = runTest {
        val settings = FakeSettingsRepository()
        val api = FakeUpdateApi(null, shouldThrow = true)
        val repo = UpdateRepositoryImpl(
            currentVersionCode = 1L,
            settingsRepository = settings,
            manifestUrlProvider = { "https://example.com/manifest.json" },
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.CheckUnavailable, status)
    }
}
