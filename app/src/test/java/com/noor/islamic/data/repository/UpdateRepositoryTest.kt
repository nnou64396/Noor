package com.noor.islamic.data.repository

import com.noor.islamic.data.remote.network.NetworkModule
import com.noor.islamic.data.remote.update.GitHubReleaseDto
import com.noor.islamic.data.remote.update.UpdateApi
import com.noor.islamic.data.remote.update.UpdateManifestDto
import com.noor.islamic.data.update.ApkDownloader
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateRepositoryTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private class FakeSettingsRepository : SettingsRepository {
        private var dismissedCode = 0L

        override suspend fun getDismissedUpdateVersionCode(): Long = dismissedCode

        override suspend fun setDismissedUpdateVersionCode(code: Long) {
            dismissedCode = code
        }

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
        private val releasesResponse: Response<List<GitHubReleaseDto>>?,
        private val manifestResponse: Response<UpdateManifestDto>? = null,
        private val shouldThrow: Boolean = false,
    ) : UpdateApi {
        override suspend fun listReleases(owner: String, repo: String): Response<List<GitHubReleaseDto>> {
            if (shouldThrow) throw java.io.IOException("Network failure")
            return releasesResponse ?: Response.error(404, "".toResponseBody(null))
        }

        override suspend fun fetchManifest(manifestUrl: String): Response<UpdateManifestDto> {
            if (shouldThrow) throw java.io.IOException("Network failure")
            return manifestResponse ?: Response.error(404, "".toResponseBody(null))
        }
    }

    @Test
    fun `networkModule provideRetrofit configures GsonConverterFactory and headers for UpdateApi`() = runTest {
        val retrofit = NetworkModule.provideRetrofit(
            baseUrl = mockWebServer.url("/").toString(),
            client = OkHttpClient(),
        )
        val api = retrofit.create(UpdateApi::class.java)

        val jsonResponse = """
            [
              {
                "tag_name": "v1.1.0",
                "name": "1.1.0",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-03-01T12:00:00Z",
                "body": "Release notes",
                "assets": [
                  {
                    "name": "app-release.apk",
                    "size": 5000000,
                    "browser_download_url": "https://github.com/test.apk",
                    "content_type": "application/vnd.android.package-archive"
                  }
                ]
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val response = api.listReleases()
        assertTrue(response.isSuccessful)
        val releases = response.body()
        assertNotNull(releases)
        assertEquals(1, releases?.size)
        assertEquals("v1.1.0", releases?.first()?.tagName)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Noor-Android", recordedRequest.getHeader("User-Agent"))
        assertEquals("application/vnd.github+json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `when api provider is null returns Disabled`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())
        val repo = UpdateRepositoryImpl(
            context = context,
            currentVersionCode = 10000L,
            settingsRepository = settings,
            apkDownloader = downloader,
            apiProvider = { null },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.Disabled, status)
    }

    @Test
    fun `when current version is up to date returns UpToDate`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())
        val release = GitHubReleaseDto(
            tagName = "v1.0.0",
            name = "1.0.0",
            draft = false,
            prerelease = false,
            publishedAt = "2026-03-01T12:00:00Z",
            body = "Release notes",
            assets = listOf(com.noor.islamic.data.remote.update.GitHubAssetDto("app-release.apk", 100L, "https://github.com/test.apk", "application/vnd.android.package-archive")),
        )
        val api = FakeUpdateApi(Response.success(listOf(release)))
        val repo = UpdateRepositoryImpl(
            context = context,
            currentVersionCode = 10000L,
            settingsRepository = settings,
            apkDownloader = downloader,
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.UpToDate, status)
    }

    @Test
    fun `when newer version available returns UpdateAvailable`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())
        val release = GitHubReleaseDto(
            tagName = "v1.1.0",
            name = "1.1.0",
            draft = false,
            prerelease = false,
            publishedAt = "2026-03-01T12:00:00Z",
            body = "Release notes",
            assets = listOf(com.noor.islamic.data.remote.update.GitHubAssetDto("app-release.apk", 100L, "https://github.com/test.apk", "application/vnd.android.package-archive")),
        )
        val api = FakeUpdateApi(Response.success(listOf(release)))
        val repo = UpdateRepositoryImpl(
            context = context,
            currentVersionCode = 10000L,
            settingsRepository = settings,
            apkDownloader = downloader,
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertTrue(status is UpdateStatus.UpdateAvailable)
        assertEquals(10100L, (status as UpdateStatus.UpdateAvailable).info.latestVersionCode)
    }

    @Test
    fun `draft and prerelease releases are ignored`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())
        val draftRelease = GitHubReleaseDto(
            tagName = "v2.0.0",
            name = "2.0.0",
            draft = true,
            prerelease = false,
            publishedAt = "2026-03-01T12:00:00Z",
            body = "Draft",
            assets = listOf(com.noor.islamic.data.remote.update.GitHubAssetDto("app-release.apk", 100L, "https://github.com/test.apk", "application/vnd.android.package-archive")),
        )
        val prerelease = GitHubReleaseDto(
            tagName = "v2.0.0-beta",
            name = "2.0.0-beta",
            draft = false,
            prerelease = true,
            publishedAt = "2026-03-01T12:00:00Z",
            body = "Prerelease",
            assets = listOf(com.noor.islamic.data.remote.update.GitHubAssetDto("app-release.apk", 100L, "https://github.com/test.apk", "application/vnd.android.package-archive")),
        )
        val api = FakeUpdateApi(Response.success(listOf(draftRelease, prerelease)))
        val repo = UpdateRepositoryImpl(
            context = context,
            currentVersionCode = 10000L,
            settingsRepository = settings,
            apkDownloader = downloader,
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.UpToDate, status)
    }

    @Test
    fun `http 403 429 500 responses return CheckUnavailable`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())

        val api403 = FakeUpdateApi(Response.error(403, "".toResponseBody(null)))
        val repo403 = UpdateRepositoryImpl(context, 10000L, settings, downloader) { api403 }
        assertEquals(UpdateStatus.CheckUnavailable, repo403.checkForUpdates())

        val api429 = FakeUpdateApi(Response.error(429, "".toResponseBody(null)))
        val repo429 = UpdateRepositoryImpl(context, 10000L, settings, downloader) { api429 }
        assertEquals(UpdateStatus.CheckUnavailable, repo429.checkForUpdates())

        val api500 = FakeUpdateApi(Response.error(500, "".toResponseBody(null)))
        val repo500 = UpdateRepositoryImpl(context, 10000L, settings, downloader) { api500 }
        assertEquals(UpdateStatus.CheckUnavailable, repo500.checkForUpdates())
    }

    @Test
    fun `when network throws exception returns CheckUnavailable safely`() = runTest {
        val settings = FakeSettingsRepository()
        val context = RuntimeEnvironment.getApplication()
        val downloader = ApkDownloader(context, OkHttpClient())
        val api = FakeUpdateApi(null, shouldThrow = true)
        val repo = UpdateRepositoryImpl(
            context = context,
            currentVersionCode = 10000L,
            settingsRepository = settings,
            apkDownloader = downloader,
            apiProvider = { api },
        )

        val status = repo.checkForUpdates()
        assertEquals(UpdateStatus.CheckUnavailable, status)
    }
}
