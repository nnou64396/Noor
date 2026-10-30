package dz.techvision.noor.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Utility for opening the official app store listing for NOOR.
 * Respects the actual installation source where known, falls back to market:// details,
 * and finally falls back to the official store web page.
 */
object StoreUpdateUtils {

    private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
    private const val RUSTORE_PACKAGE_1 = "ru.zdevs.zstore"
    private const val RUSTORE_PACKAGE_2 = "ru.store.appupdate"
    private const val HUAWEI_APPMARKET_PACKAGE = "com.huawei.appmarket"
    private const val SAMSUNG_STORE_PACKAGE = "com.sec.android.app.samsungapps"

    /**
     * Opens the official app store listing for application updates.
     */
    fun openStoreListing(context: Context) {
        val packageName = context.packageName
        val installerPackage = getInstallerPackageName(context)

        val intent = when (installerPackage) {
            GOOGLE_PLAY_PACKAGE -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName"),
            ).apply { setPackage(GOOGLE_PLAY_PACKAGE) }

            HUAWEI_APPMARKET_PACKAGE -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("appmarket://details?id=$packageName"),
            ).apply { setPackage(HUAWEI_APPMARKET_PACKAGE) }

            SAMSUNG_STORE_PACKAGE -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("samsungapps://ProductDetail/$packageName"),
            ).apply { setPackage(SAMSUNG_STORE_PACKAGE) }

            RUSTORE_PACKAGE_1, RUSTORE_PACKAGE_2 -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://apps.rustore.ru/app/$packageName"),
            )

            else -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName"),
            )
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Generic fallback to browser web listing on Google Play / official store
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(webIntent) }
        }
    }

    private fun getInstallerPackageName(context: Context): String? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
    }
}
