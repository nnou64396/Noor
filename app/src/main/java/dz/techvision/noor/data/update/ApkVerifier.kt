package dz.techvision.noor.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * Testable boundary around APK integrity verification.
 */
fun interface ApkVerificationService {
    fun verifyApk(
        context: Context,
        apkFile: File,
        expectedVersionCode: Long,
        expectedSha256: String,
        actualSha256: String,
    ): ApkVerifier.VerificationResult
}

object ApkVerifier : ApkVerificationService {
    private const val EXPECTED_PACKAGE_NAME = "dz.techvision.noor"
    private const val EXPECTED_CERT_SHA256 = "6c4e68e4f8a1bc982a09611886f0eda25893642e24f14fa77642ea1a6b3ef83d"

    data class VerificationResult(
        val success: Boolean,
        val error: String? = null,
        val versionCode: Long = 0L,
        val versionName: String? = null,
    )

    override fun verifyApk(
        context: Context,
        apkFile: File,
        expectedVersionCode: Long,
        expectedSha256: String,
        actualSha256: String,
    ): VerificationResult {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return VerificationResult(false, "APK file not readable or does not exist")
        }

        if (!expectedSha256.equals(actualSha256, ignoreCase = true)) {
            return VerificationResult(false, "SHA-256 checksum mismatch")
        }

        val pm = context.packageManager
        val archiveInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            }
        }.getOrNull()

        if (archiveInfo == null) {
            return VerificationResult(false, "Failed to parse APK archive package info")
        }

        val packageName = archiveInfo.packageName
        if (packageName != EXPECTED_PACKAGE_NAME) {
            return VerificationResult(false, "Invalid package name: $packageName")
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        val versionName = archiveInfo.versionName

        if (versionCode != expectedVersionCode) {
            return VerificationResult(false, "Version code mismatch: expected $expectedVersionCode, got $versionCode")
        }

        val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.signingInfo?.let { signingInfo ->
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            }
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.signatures
        }

        if (certs.isNullOrEmpty()) {
            return VerificationResult(false, "APK has no signing certificates")
        }

        val matchedCert = certs.any { sig ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex.lowercase(Locale.ROOT) == EXPECTED_CERT_SHA256.lowercase(Locale.ROOT)
        }

        if (!matchedCert) {
            return VerificationResult(false, "Signing certificate fingerprint does not match trusted production identity")
        }

        return VerificationResult(true, versionCode = versionCode, versionName = versionName)
    }
}

