package dz.techvision.noor.data.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Testable boundary around the Android package installer.
 */
interface ApkInstallerService {
    fun canRequestPackageInstalls(context: Context): Boolean
    fun installApk(context: Context, apkFile: File): ApkInstaller.InstallOutcome
}

/**
 * Opens the verified APK with the system package installer.
 *
 * Launching an intent does not prove that installation succeeded: the user can
 * cancel, and Android does not reliably report that outcome. The flow therefore
 * stays at [dz.techvision.noor.domain.model.UpdateStatus.ReadyToInstall] after a
 * successful launch and only reports failures it can actually observe.
 */
object ApkInstaller : ApkInstallerService {

    sealed interface InstallOutcome {
        data object Launched : InstallOutcome
        data object PermissionRequired : InstallOutcome
        data class Failure(val reason: String) : InstallOutcome
    }

    override fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    override fun installApk(context: Context, apkFile: File): InstallOutcome {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return InstallOutcome.Failure(
                "The verified update file is no longer available. Please download the update again.",
            )
        }

        if (!canRequestPackageInstalls(context)) {
            return InstallOutcome.PermissionRequired
        }

        val intent = try {
            buildInstallIntent(context, apkFile)
        } catch (e: IllegalArgumentException) {
            // FileProvider throws when the file is outside the configured roots.
            return InstallOutcome.Failure(
                "The verified update file could not be shared securely. Please download the update again.",
            )
        } catch (t: Throwable) {
            return InstallOutcome.Failure(
                "The package installer could not be prepared. Please try again.",
            )
        }

        // Do not pre-resolve the intent: package visibility filtering can hide
        // the system installer from resolveActivity() on Android 11+ even
        // though startActivity() would succeed. Launch and handle the real
        // ActivityNotFoundException instead.
        return try {
            context.startActivity(intent)
            InstallOutcome.Launched
        } catch (e: ActivityNotFoundException) {
            InstallOutcome.Failure(
                "No compatible package installer is available on this device.",
            )
        } catch (e: SecurityException) {
            InstallOutcome.Failure(
                "Android blocked the installation request. Check the install permission and try again.",
            )
        } catch (t: Throwable) {
            InstallOutcome.Failure(
                "The package installer could not be opened. Please try again.",
            )
        }
    }

    /**
     * Builds the install intent with a content URI, the APK MIME type, and a
     * read grant. The URI resolver is injectable so the security-relevant
     * intent flags can be regression-tested; production always uses
     * FileProvider with the declared authority.
     */
    fun buildInstallIntent(
        context: Context,
        apkFile: File,
        resolveUri: (Context, File) -> Uri = { ctx, file ->
            FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file,
            )
        },
    ): Intent {
        val uri = resolveUri(context, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Settings screen where the user grants "install unknown apps" for NOOR. */
    fun buildInstallPermissionSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    /** Fallback when a device does not expose the unknown-app-sources screen. */
    fun buildAppDetailsSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
}

