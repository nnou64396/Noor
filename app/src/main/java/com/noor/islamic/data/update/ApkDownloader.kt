package com.noor.islamic.data.update

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class ApkDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val MAX_FILE_SIZE = 50L * 1024L * 1024L // 50 MB
        private val TRUSTED_HOSTS = listOf("github.com", "raw.githubusercontent.com", "objects.githubusercontent.com")
    }

    data class DownloadResult(
        val success: Boolean,
        val file: File? = null,
        val sha256Hex: String? = null,
        val error: String? = null,
    )

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit = {},
    ): DownloadResult {
        val uri = runCatching { Uri.parse(downloadUrl) }.getOrNull()
        if (uri == null || uri.scheme?.lowercase(Locale.ROOT) != "https") {
            return DownloadResult(success = false, error = "Invalid or non-HTTPS download URL")
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: return DownloadResult(success = false, error = "Missing host in download URL")
        val isTrusted = TRUSTED_HOSTS.any { trusted -> host == trusted || host.endsWith(".$trusted") }
        if (!isTrusted) {
            return DownloadResult(success = false, error = "Untrusted download host: $host")
        }

        val outputFile = File(context.cacheDir, "noor_update.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val request = Request.Builder().url(downloadUrl).get().build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return DownloadResult(success = false, error = "HTTP error: ${response.code}")
                }

                val body = response.body ?: return DownloadResult(success = false, error = "Empty response body")
                val contentLength = body.contentLength()
                if (contentLength > MAX_FILE_SIZE) {
                    return DownloadResult(success = false, error = "File size exceeds maximum allowed limit (50MB)")
                }

                val digest = MessageDigest.getInstance("SHA-256")
                FileOutputStream(outputFile).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesDownloaded = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            bytesDownloaded += read
                            if (bytesDownloaded > MAX_FILE_SIZE) {
                                outputFile.delete()
                                return DownloadResult(success = false, error = "Download exceeded maximum allowed size")
                            }
                            fos.write(buffer, 0, read)
                            digest.update(buffer, 0, read)

                            if (contentLength > 0L) {
                                val progress = bytesDownloaded.toFloat() / contentLength.toFloat()
                                onProgress(progress.coerceIn(0f, 1f))
                            }
                        }
                        fos.flush()
                    }
                }

                if (!outputFile.exists() || outputFile.length() == 0L) {
                    return DownloadResult(success = false, error = "Downloaded file is empty or missing")
                }

                val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }
                DownloadResult(success = true, file = outputFile, sha256Hex = sha256Hex)
            }
        } catch (e: CancellationException) {
            outputFile.delete()
            throw e
        } catch (t: Throwable) {
            outputFile.delete()
            DownloadResult(success = false, error = "Download failed: ${t.localizedMessage ?: t.javaClass.simpleName}")
        }
    }
}
