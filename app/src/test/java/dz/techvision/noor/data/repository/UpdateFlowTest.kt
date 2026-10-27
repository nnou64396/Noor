package dz.techvision.noor.data.repository

import android.content.Context
import dz.techvision.noor.data.update.ApkDownloadService
import dz.techvision.noor.data.update.ApkInstaller
import dz.techvision.noor.data.update.ApkInstallerService
import dz.techvision.noor.data.update.ApkVerificationService
import dz.techvision.noor.data.update.ApkVerifier
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Download/verify/install orchestration over fakes. These cover the failure,
 * cancellation, retry, and permission transitions that the discovery-only tests
 * do not exercise.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateFlowTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private val info = AppUpdateInfo(
        latestVersion = "1.1.3",
        latestVersionCode = 10100L,
        minimumSupportedVersionCode = 0L,
        mandatory = false,
        sha256 = "expected-sha",
        releaseNotes = mapOf("en" to "Notes"),
        downloadUrl = "https://github.com/nnou64396/Noor/releases/download/v1.1.3/app-release.apk",
    )

    private fun apkFile(): File = File(context.cacheDir, "noor_update.apk").apply {
        parentFile?.mkdirs()
        writeBytes(byteArrayOf(1, 2, 3, 4))
    }

    private fun repository(
        downloader: ApkDownloadService,
        verifier: ApkVerificationService = FakeVerifier(),
        installer: ApkInstallerService = FakeInstaller(),
    ): UpdateRepositoryImpl = UpdateRepositoryImpl(
        context = context,
        currentVersionCode = 10000L,
        settingsRepository = FakeSettingsRepository(),
        apkDownloader = downloader,
        apiProvider = { null },
        apkVerifier = verifier,
        apkInstaller = installer,
    )

    private suspend fun readyRepository(installer: FakeInstaller): UpdateRepositoryImpl {
        val repo = repository(
            downloader = FakeDownloader { progress ->
                progress(0.5f)
                ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
            },
            installer = installer,
        )
        assertTrue(repo.downloadAndVerifyUpdate(info))
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
        return repo
    }

    @Test
    fun `successful download verifies before ReadyToInstall`() = runTest {
        val verifier = FakeVerifier()
        val repo = repository(
            downloader = FakeDownloader {
                ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
            },
            verifier = verifier,
        )

        assertTrue(repo.downloadAndVerifyUpdate(info))
        assertEquals(1, verifier.calls)
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
    }

    @Test
    fun `download progress is exposed while downloading`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val downloader = FakeDownloader { progress ->
            progress(0.4f)
            gate.await()
            ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
        }
        val repo = repository(downloader)

        val job = launch { repo.downloadAndVerifyUpdate(info) }
        runCurrent()

        assertEquals(UpdateStatus.Downloading(0.4f), repo.updateStatus.value)
        assertEquals(0.4f, repo.downloadProgress.value)

        gate.complete(Unit)
        job.join()
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
        assertEquals(1f, repo.downloadProgress.value)
    }

    @Test
    fun `repeated download requests do not start a duplicate download`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val downloader = FakeDownloader {
            gate.await()
            ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
        }
        val repo = repository(downloader)

        val first = launch { repo.downloadAndVerifyUpdate(info) }
        runCurrent()

        assertFalse(repo.downloadAndVerifyUpdate(info))
        assertEquals(1, downloader.calls)

        gate.complete(Unit)
        first.join()
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
    }

    @Test
    fun `http failure produces a recoverable verification failure`() = runTest {
        val repo = repository(
            FakeDownloader { progress ->
                progress(0.3f)
                ApkDownloadService.DownloadResult(
                    failure = ApkDownloadService.Failure.Http(503),
                )
            },
        )

        assertFalse(repo.downloadAndVerifyUpdate(info))
        val status = repo.updateStatus.value
        assertTrue(status is UpdateStatus.VerificationFailed)
        assertTrue((status as UpdateStatus.VerificationFailed).reason.contains("HTTP 503"))
        assertEquals(0f, repo.downloadProgress.value)
    }

    @Test
    fun `sha256 mismatch blocks installation and deletes the file`() = runTest {
        val file = apkFile()
        val repo = repository(
            downloader = FakeDownloader {
                ApkDownloadService.DownloadResult(file = file, sha256Hex = "different-sha")
            },
            verifier = ApkVerifier,
        )

        assertFalse(repo.downloadAndVerifyUpdate(info))
        val status = repo.updateStatus.value
        assertTrue(status is UpdateStatus.VerificationFailed)
        assertTrue((status as UpdateStatus.VerificationFailed).reason.contains("SHA-256"))
        assertFalse(file.exists())
    }

    @Test
    fun `unexpected verifier exception recovers from the downloading state`() = runTest {
        val file = apkFile()
        val repo = repository(
            downloader = FakeDownloader {
                ApkDownloadService.DownloadResult(file = file, sha256Hex = info.sha256)
            },
            verifier = ThrowingVerifier(),
        )

        assertFalse(repo.downloadAndVerifyUpdate(info))
        assertFalse(repo.updateStatus.value is UpdateStatus.Downloading)
        assertTrue(repo.updateStatus.value is UpdateStatus.VerificationFailed)
        assertFalse(file.exists())
    }

    @Test
    fun `cancellation does not leave the UI stuck in Downloading`() = runTest {
        val repo = repository(
            FakeDownloader { progress ->
                progress(0.2f)
                awaitCancellation()
            },
        )

        val job = launch { repo.downloadAndVerifyUpdate(info) }
        runCurrent()
        assertTrue(repo.updateStatus.value is UpdateStatus.Downloading)

        job.cancelAndJoin()

        assertFalse(repo.updateStatus.value is UpdateStatus.Downloading)
        assertTrue(repo.updateStatus.value is UpdateStatus.UpdateAvailable)
    }

    @Test
    fun `install without permission reports the permission-required flow`() = runTest {
        val installer = FakeInstaller(permissionGranted = false)
        val repo = readyRepository(installer)

        repo.installUpdate(context)

        assertTrue(repo.updateStatus.value is UpdateStatus.InstallPermissionRequired)
        assertEquals(0, installer.installCalls)
    }

    @Test
    fun `granting install permission resumes installation`() = runTest {
        val installer = FakeInstaller(permissionGranted = false)
        val repo = readyRepository(installer)
        repo.installUpdate(context)
        assertTrue(repo.updateStatus.value is UpdateStatus.InstallPermissionRequired)

        installer.permissionGranted = true
        assertTrue(repo.onInstallPermissionResult(context))

        assertEquals(1, installer.installCalls)
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
    }

    @Test
    fun `installer launch failure is reported safely`() = runTest {
        val installer = FakeInstaller(
            outcome = ApkInstaller.InstallOutcome.Failure("No compatible package installer is available on this device."),
        )
        val repo = readyRepository(installer)

        repo.installUpdate(context)

        val status = repo.updateStatus.value
        assertTrue(status is UpdateStatus.InstallFailed)
        assertEquals(
            "No compatible package installer is available on this device.",
            (status as UpdateStatus.InstallFailed).reason,
        )
    }

    @Test
    fun `retry after download failure performs a real download`() = runTest {
        var attempt = 0
        val downloader = FakeDownloader {
            attempt += 1
            if (attempt == 1) {
                ApkDownloadService.DownloadResult(
                    failure = ApkDownloadService.Failure.Network,
                )
            } else {
                ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
            }
        }
        val repo = repository(downloader)

        assertFalse(repo.downloadAndVerifyUpdate(info))
        assertTrue(repo.updateStatus.value is UpdateStatus.VerificationFailed)

        repo.retryUpdate(info, context)

        assertEquals(2, downloader.calls)
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
    }

    @Test
    fun `retry after install failure re-launches the verified APK`() = runTest {
        val installer = FakeInstaller(
            outcome = ApkInstaller.InstallOutcome.Failure("The package installer could not be opened. Please try again."),
        )
        val repo = readyRepository(installer)
        repo.installUpdate(context)
        assertTrue(repo.updateStatus.value is UpdateStatus.InstallFailed)

        installer.outcome = ApkInstaller.InstallOutcome.Launched
        repo.retryUpdate(info, context)

        assertEquals(2, installer.installCalls)
        assertTrue(repo.updateStatus.value is UpdateStatus.ReadyToInstall)
    }

    @Test
    fun `dismissal does not cancel an active download`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = repository(
            FakeDownloader {
                gate.await()
                ApkDownloadService.DownloadResult(file = apkFile(), sha256Hex = info.sha256)
            },
        )

        val job = launch { repo.downloadAndVerifyUpdate(info) }
        runCurrent()

        repo.dismissUpdate(info.latestVersionCode)
        assertTrue(repo.updateStatus.value is UpdateStatus.Downloading)

        gate.complete(Unit)
        job.join()
    }

    private class FakeDownloader(
        private val block: suspend (progress: (Float) -> Unit) -> ApkDownloadService.DownloadResult,
    ) : ApkDownloadService {
        var calls = 0

        override suspend fun downloadApk(
            downloadUrl: String,
            onProgress: (Float) -> Unit,
        ): ApkDownloadService.DownloadResult {
            calls += 1
            return block(onProgress)
        }
    }

    private class FakeVerifier(
        private val result: ApkVerifier.VerificationResult = ApkVerifier.VerificationResult(true),
    ) : ApkVerificationService {
        var calls = 0

        override fun verifyApk(
            context: Context,
            apkFile: File,
            expectedVersionCode: Long,
            expectedSha256: String,
            actualSha256: String,
        ): ApkVerifier.VerificationResult {
            calls += 1
            return result
        }
    }

    private class ThrowingVerifier : ApkVerificationService {
        override fun verifyApk(
            context: Context,
            apkFile: File,
            expectedVersionCode: Long,
            expectedSha256: String,
            actualSha256: String,
        ): ApkVerifier.VerificationResult = error("Unexpected verifier crash")
    }

    private class FakeInstaller(
        var permissionGranted: Boolean = true,
        var outcome: ApkInstaller.InstallOutcome = ApkInstaller.InstallOutcome.Launched,
    ) : ApkInstallerService {
        var installCalls = 0

        override fun canRequestPackageInstalls(context: Context): Boolean = permissionGranted

        override fun installApk(
            context: Context,
            apkFile: File,
        ): ApkInstaller.InstallOutcome {
            installCalls += 1
            return outcome
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val dismissedCode = MutableStateFlow(0L)

        override suspend fun getDismissedUpdateVersionCode(): Long = dismissedCode.value

        override suspend fun setDismissedUpdateVersionCode(code: Long) {
            dismissedCode.value = code
        }

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
    }
}

