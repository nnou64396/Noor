package com.noor.islamic.data.update

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/**
 * Boundary around the APK download so the update flow can be exercised without
 * performing real network calls.
 */
interface ApkDownloadService {
    data class DownloadResult(
        val file: File? = null,
        val sha256Hex: String? = null,
        val failure: Failure? = null,
    ) {
        val success: Boolean
            get() = file != null && sha256Hex != null && failure == null
    }

    /**
     * Typed failures keep implementation details (URLs, exception messages,
     * local paths) out of the UI. The repository maps these to safe copy.
     */
    sealed interface Failure {
        data object InvalidUrl : Failure
        data object UntrustedHost : Failure
        data class Http(val code: Int) : Failure
        data object EmptyResponse : Failure
        data object TooLarge : Failure
        data object Network : Failure
        data object FileAccess : Failure
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit = {},
    ): DownloadResult
}

class ApkDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) : ApkDownloadService {
    companion object {
        private const val MAX_FILE_SIZE = 50L * 1024L * 1024L // 50 MB
        private val TRUSTED_HOSTS = listOf("github.com", "raw.githubusercontent.com", "objects.githubusercontent.com")
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): ApkDownloadService.DownloadResult = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(downloadUrl) }.getOrNull()
        if (uri == null || !uri.scheme.equals("https", ignoreCase = true)) {
            return@withContext ApkDownloadService.DownloadResult(
                failure = ApkDownloadService.Failure.InvalidUrl,
            )
        }

        val host = uri.host?.lowercase(Locale.ROOT)
            ?: return@withContext ApkDownloadService.DownloadResult(
                failure = ApkDownloadService.Failure.InvalidUrl,
            )
        val isTrusted = TRUSTED_HOSTS.any { trusted -> host == trusted || host.endsWith(".$trusted") }
        if (!isTrusted) {
            return@withContext ApkDownloadService.DownloadResult(
                failure = ApkDownloadService.Failure.UntrustedHost,
            )
        }

        val outputFile = File(context.cacheDir, "noor_update.apk")
        runCatching { outputFile.delete() }

        fun fail(failure: ApkDownloadService.Failure): ApkDownloadService.DownloadResult {
            runCatching { outputFile.delete() }
            return ApkDownloadService.DownloadResult(failure = failure)
        }

        val request = Request.Builder().url(downloadUrl).get().build()
        val call = okHttpClient.newCall(request)

        // OkHttp's blocking execute() does not observe coroutine cancellation.
        // Cancel the call when the surrounding job is cancelled so a screen
        // teardown cannot leave the download running indefinitely.
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext fail(ApkDownloadService.Failure.Http(response.code))
                }

                val body = response.body ?: return@withContext fail(ApkDownloadService.Failure.EmptyResponse)
                val contentLength = body.contentLength()
                if (contentLength > MAX_FILE_SIZE) {
                    return@withContext fail(ApkDownloadService.Failure.TooLarge)
                }

                val digest = MessageDigest.getInstance("SHA-256")
                FileOutputStream(outputFile).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesDownloaded = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            bytesDownloaded += read
                            if (bytesDownloaded > MAX_FILE_SIZE) {
                                return@withContext fail(ApkDownloadService.Failure.TooLarge)
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
                    return@withContext fail(ApkDownloadService.Failure.EmptyResponse)
                }

                val sha256Hex = digest.digest().joinToString("") { "%02x".format(it) }
                ApkDownloadService.DownloadResult(file = outputFile, sha256Hex = sha256Hex)
            }
        } catch (e: CancellationException) {
            runCatching { outputFile.delete() }
            throw e
        } catch (e: FileNotFoundException) {
            fail(ApkDownloadService.Failure.FileAccess)
        } catch (t: Throwable) {
            // If the blocking call was interrupted by cancellation, surface it
            // as cancellation instead of a recoverable network error.
            currentCoroutineContext().ensureActive()
            fail(ApkDownloadService.Failure.Network)
        } finally {
            cancellationHandle?.dispose()
        }
    }
}
