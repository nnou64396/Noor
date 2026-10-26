package com.noor.islamic.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateStatusTest {

    private val info = AppUpdateInfo(
        latestVersion = "1.1.3",
        latestVersionCode = 10100L,
        minimumSupportedVersionCode = 0L,
        sha256 = "expected-sha",
        downloadUrl = "https://github.com/example/app-release.apk",
    )

    @Test
    fun `all actionable update states keep the banner visible`() {
        val actionable = listOf(
            UpdateStatus.UpdateAvailable(info),
            UpdateStatus.UpdateRequired(info),
            UpdateStatus.Downloading(0.25f),
            UpdateStatus.VerificationFailed(info, "checksum mismatch"),
            UpdateStatus.ReadyToInstall(info),
            UpdateStatus.InstallPermissionRequired(info),
            UpdateStatus.InstallFailed(info, "installer unavailable"),
        )

        assertTrue(actionable.all { it.isActionableUpdate })
    }

    @Test
    fun `passive update states do not show the banner`() {
        val passive = listOf(
            UpdateStatus.UpToDate,
            UpdateStatus.Disabled,
            UpdateStatus.CheckUnavailable,
        )

        assertTrue(passive.none { it.isActionableUpdate })
        assertFalse(UpdateStatus.UpToDate.isActionableUpdate)
    }
}
