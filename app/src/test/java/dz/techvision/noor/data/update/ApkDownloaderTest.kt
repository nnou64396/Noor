package dz.techvision.noor.data.update

import android.content.Context
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Exercises the real downloader against synthetic OkHttp responses so the
 * HTTP -> typed-failure mapping, checksum computation, and file handling are
 * covered without network access.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ApkDownloaderTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private val apkBytes = "verified apk bytes".toByteArray()

    private val url = "https://github.com/nnou64396/Noor/releases/download/v1.1.3/app-release.apk"

    private fun client(handler: (Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain -> handler(chain.request()) }
            .build()

    private fun response(request: Request, code: Int, body: ByteArray): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(
                body.toResponseBody(
                    "application/vnd.android.package-archive".toMediaType(),
                ),
            )
            .build()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Test
    fun `successful download writes the file and reports its sha256`() = runTest {
        val downloader = ApkDownloader(context, client { response(it, 200, apkBytes) })
        val progress = mutableListOf<Float>()

        val result = downloader.downloadApk(url) { progress += it }

        assertTrue(result.success)
        assertEquals(sha256(apkBytes), result.sha256Hex)
        assertTrue(result.file!!.exists())
        assertTrue(apkBytes.contentEquals(result.file!!.readBytes()))
        assertTrue(progress.isNotEmpty())
        assertTrue(progress.all { it in 0f..1f })
        assertEquals(1f, progress.last())
    }

    @Test
    fun `http error maps to a typed failure and leaves no file`() = runTest {
        val downloader = ApkDownloader(context, client { response(it, 404, ByteArray(0)) })

        val result = downloader.downloadApk(url)

        assertFalse(result.success)
        assertNull(result.file)
        assertEquals(ApkDownloadService.Failure.Http(404), result.failure)
    }

    @Test
    fun `empty body is rejected`() = runTest {
        val downloader = ApkDownloader(context, client { response(it, 200, ByteArray(0)) })

        val result = downloader.downloadApk(url)

        assertFalse(result.success)
        assertEquals(ApkDownloadService.Failure.EmptyResponse, result.failure)
    }

    @Test
    fun `untrusted host is rejected without calling the network`() = runTest {
        var called = false
        val downloader = ApkDownloader(
            context,
            client {
                called = true
                response(it, 200, apkBytes)
            },
        )

        val result = downloader.downloadApk("https://example.com/app-release.apk")

        assertFalse(called)
        assertEquals(ApkDownloadService.Failure.UntrustedHost, result.failure)
    }

    @Test
    fun `non-https url is rejected`() = runTest {
        val downloader = ApkDownloader(context, client { response(it, 200, apkBytes) })

        val result = downloader.downloadApk("http://github.com/app-release.apk")

        assertEquals(ApkDownloadService.Failure.InvalidUrl, result.failure)
    }

    @Test
    fun `existing cached apk is replaced by the new download`() = runTest {
        val cached = context.cacheDir.resolve("noor_update.apk")
        cached.writeText("stale")
        val newBytes = "new apk bytes".toByteArray()
        val downloader = ApkDownloader(context, client { response(it, 200, newBytes) })

        val result = downloader.downloadApk(url)

        assertTrue(result.success)
        assertTrue(newBytes.contentEquals(cached.readBytes()))
    }
}

