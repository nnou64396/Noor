package com.noor.islamic.ui.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.noor.islamic.domain.model.AppUpdateInfo
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.domain.model.UpdateStatus
import com.noor.islamic.presentation.settings.SettingsScreen
import com.noor.islamic.ui.theme.NoorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdateBannerCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val info = AppUpdateInfo(
        latestVersion = "1.1.3",
        latestVersionCode = 10100L,
        minimumSupportedVersionCode = 0L,
        sha256 = "expected-sha",
        downloadUrl = "https://github.com/nnou64396/Noor/releases/download/v1.1.3/app-release.apk",
    )

    private fun setCard(
        status: UpdateStatus,
        onDismiss: (Long) -> Unit = {},
        onDownloadAndVerify: (AppUpdateInfo) -> Unit = {},
        onInstall: (Context) -> Unit = {},
        onRetry: (AppUpdateInfo, Context) -> Unit = { _, _ -> },
        onOpenInstallSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            NoorTheme {
                UpdateBannerCard(
                    updateStatus = status,
                    onDismiss = onDismiss,
                    onDownloadAndVerify = onDownloadAndVerify,
                    onInstall = onInstall,
                    onRetry = onRetry,
                    onOpenInstallSettings = onOpenInstallSettings,
                )
            }
        }
    }

    @Test
    fun `get update button invokes the download callback`() {
        var captured: AppUpdateInfo? = null
        setCard(
            status = UpdateStatus.UpdateAvailable(info),
            onDownloadAndVerify = { captured = it },
        )

        composeRule.onNodeWithTag("update_get_button").performClick()

        assertEquals(info, captured)
    }

    @Test
    fun `install button is hidden until verification succeeds`() {
        setCard(status = UpdateStatus.UpdateAvailable(info))

        composeRule.onNodeWithTag("update_get_button").assertExists()
        composeRule.onNodeWithTag("update_install_button").assertDoesNotExist()
    }

    @Test
    fun `install button invokes the install callback with a context`() {
        var captured: Context? = null
        setCard(
            status = UpdateStatus.ReadyToInstall(info),
            onInstall = { captured = it },
        )

        composeRule.onNodeWithTag("update_install_button").performClick()

        assertNotNull(captured)
    }

    @Test
    fun `retry button invokes a real retry callback after verification failure`() {
        var captured: AppUpdateInfo? = null
        setCard(
            status = UpdateStatus.VerificationFailed(info, "Checksum mismatch"),
            onRetry = { update, _ -> captured = update },
        )

        composeRule.onNodeWithTag("update_retry_button").performClick()

        assertEquals(info, captured)
    }

    @Test
    fun `permission button opens the install permission settings`() {
        var opened = false
        setCard(
            status = UpdateStatus.InstallPermissionRequired(info),
            onOpenInstallSettings = { opened = true },
        )

        composeRule.onNodeWithTag("update_open_settings_button").performClick()

        assertTrue(opened)
    }

    @Test
    fun `every actionable state keeps a visible update card`() {
        var status by mutableStateOf<UpdateStatus>(UpdateStatus.UpdateAvailable(info))
        composeRule.setContent {
            NoorTheme {
                UpdateBannerCard(
                    updateStatus = status,
                    onDismiss = {},
                    onDownloadAndVerify = {},
                    onInstall = {},
                    onRetry = { _, _ -> },
                    onOpenInstallSettings = {},
                )
            }
        }

        val states = listOf(
            UpdateStatus.UpdateAvailable(info),
            UpdateStatus.UpdateRequired(info),
            UpdateStatus.Downloading(0.5f),
            UpdateStatus.VerificationFailed(info, "Checksum mismatch"),
            UpdateStatus.ReadyToInstall(info),
            UpdateStatus.InstallPermissionRequired(info),
            UpdateStatus.InstallFailed(info, "Installer unavailable"),
        )

        states.forEach { state ->
            status = state
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("update_banner_card").assertExists()
        }
    }

    @Test
    fun `settings screen shows downloading and verification failure cards`() {
        var status by mutableStateOf<UpdateStatus>(UpdateStatus.Downloading(0.5f))
        composeRule.setContent {
            NoorTheme {
                SettingsScreen(
                    language = Language.ENGLISH,
                    themeMode = ThemeMode.SYSTEM,
                    onLanguageSelected = {},
                    onThemeSelected = {},
                    updateStatus = status,
                    onCheckForUpdates = {},
                    onDismissUpdate = {},
                    onDownloadAndVerifyUpdate = {},
                    onInstallUpdate = {},
                    onRetryUpdate = { _, _ -> },
                    onOpenInstallSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("update_banner_card").assertExists()

        status = UpdateStatus.VerificationFailed(info, "Checksum mismatch")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("update_banner_card").assertExists()
    }

    @Test
    fun `settings get update button invokes the wired download callback`() {
        var captured: AppUpdateInfo? = null
        composeRule.setContent {
            NoorTheme {
                SettingsScreen(
                    language = Language.ENGLISH,
                    themeMode = ThemeMode.SYSTEM,
                    onLanguageSelected = {},
                    onThemeSelected = {},
                    updateStatus = UpdateStatus.UpdateAvailable(info),
                    onCheckForUpdates = {},
                    onDismissUpdate = {},
                    onDownloadAndVerifyUpdate = { captured = it },
                    onInstallUpdate = {},
                    onRetryUpdate = { _, _ -> },
                    onOpenInstallSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("update_get_button").performScrollTo().performClick()

        assertEquals(info, captured)
    }
}
