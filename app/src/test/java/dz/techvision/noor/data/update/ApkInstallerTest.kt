package dz.techvision.noor.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dz.techvision.noor.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ApkInstallerTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun apkFile(): File = File(context.cacheDir, "noor_update.apk").apply {
        parentFile?.mkdirs()
        writeBytes(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun `install intent exposes a content URI with the APK MIME type and read grant`() {
        val expectedUri = Uri.parse(
            "content://dz.techvision.noor.fileprovider/update_cache/noor_update.apk",
        )

        val intent = ApkInstaller.buildInstallIntent(context, apkFile()) { _, _ -> expectedUri }

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(expectedUri, intent.data)
        assertEquals("content", intent.data?.scheme)
        assertEquals("dz.techvision.noor.fileprovider", intent.data?.authority)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `file provider config exposes the cache directory used for the APK`() {
        val parser = context.resources.getXml(R.xml.file_paths)
        var rootTag: String? = null
        var path: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                rootTag = parser.name
                path = parser.getAttributeValue(null, "path")
            }
        }

        assertEquals("cache-path", rootTag)
        assertEquals(".", path)
    }

    @Test
    fun `permission settings intent opens unknown app sources for the NOOR package`() {
        val intent = ApkInstaller.buildInstallPermissionSettingsIntent(context)

        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:dz.techvision.noor", intent.dataString)
    }

    @Test
    fun `install without permission reports permission required`() {
        Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(false)

        val outcome = ApkInstaller.installApk(context, apkFile())

        assertEquals(ApkInstaller.InstallOutcome.PermissionRequired, outcome)
    }

    @Test
    fun `missing APK reports a safe failure instead of launching the installer`() {
        val outcome = ApkInstaller.installApk(
            context,
            File(context.cacheDir, "does-not-exist.apk"),
        )

        assertTrue(outcome is ApkInstaller.InstallOutcome.Failure)
        assertTrue(
            (outcome as ApkInstaller.InstallOutcome.Failure).reason.contains("download"),
        )
    }

    @Test
    fun `app details fallback intent targets the NOOR package`() {
        val intent = ApkInstaller.buildAppDetailsSettingsIntent(context)

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:dz.techvision.noor", intent.dataString)
    }
}

