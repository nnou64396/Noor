package dz.techvision.noor.data.remote.network

import android.content.pm.ApplicationInfo
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Verifies the release-safe HTTP logging policy: nothing is logged unless the
 * build is debuggable, and neither the precise coordinates nor any credential
 * reach the log even when it is.
 */
class NetworkLoggingTest {

    /** Minimal chain that returns a canned response for the request it is given. */
    private class StubChain(private val original: Request) : Interceptor.Chain {
        private val response = Response.Builder()
            .request(original)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Set-Cookie", "session=super-secret-set-cookie")
            .body("{}".toResponseBody(null))
            .build()

        override fun request(): Request = original
        override fun proceed(request: Request): Response = response
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 0
        override fun readTimeoutMillis(): Int = 0
        override fun writeTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    /** Runs a request through the real interceptor and returns what it logged. */
    private fun logFor(
        url: String,
        headers: Map<String, String> = emptyMap(),
        level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    ): String {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        val captured = StringBuilder()
        val sink = HttpLoggingInterceptor.Logger { message -> captured.appendLine(message) }
        NetworkModule.createLoggingInterceptor(sink).apply { this.level = level }.intercept(StubChain(request))
        return captured.toString()
    }

    private val prayerTimingsUrl =
        "https://api.aladhan.com/v1/timings/24-09-2026?latitude=36.753800&longitude=3.058800&method=3&school=0"

    @Test
    fun `the default client installs no logging interceptor`() {
        val client = NetworkModule.provideOkHttpClient()

        assertTrue(
            "the default OkHttpClient must not log; found ${client.interceptors}",
            client.interceptors.none { it is HttpLoggingInterceptor },
        )
    }

    @Test
    fun `logging is enabled only for a debuggable build`() {
        assertTrue(NetworkModule.shouldLogHttp(ApplicationInfo.FLAG_DEBUGGABLE))
        assertFalse(NetworkModule.shouldLogHttp(0))
        assertFalse(
            "a release build (debuggable cleared) must not log",
            NetworkModule.shouldLogHttp(ApplicationInfo.FLAG_DEBUGGABLE.inv()),
        )
    }

    @Test
    fun `an explicitly enabled client does install the interceptor`() {
        val client = NetworkModule.provideOkHttpClient(logging = true)

        assertEquals(1, client.interceptors.count { it is HttpLoggingInterceptor })
    }

    @Test
    fun `the enabled interceptor redacts latitude and longitude`() {
        val logged = logFor(prayerTimingsUrl)

        assertTrue("the request should still be logged: $logged", logged.contains("api.aladhan.com"))
        assertFalse("latitude leaked into the log: $logged", logged.contains("36.753800"))
        assertFalse("longitude leaked into the log: $logged", logged.contains("3.058800"))

        // The parameter names survive, so the log stays useful for debugging.
        assertTrue("the latitude parameter should remain visible: $logged", logged.contains("latitude="))
        assertTrue("the longitude parameter should remain visible: $logged", logged.contains("longitude="))
    }

    @Test
    fun `redaction does not depend on the coordinate value`() {
        val logged = logFor(
            "https://api.aladhan.com/v1/timings/24-09-2026" +
                "?latitude=-33.8688197&longitude=151.2092955&method=3&school=0",
        )

        assertFalse("latitude leaked into the log: $logged", logged.contains("-33.8688197"))
        assertFalse("longitude leaked into the log: $logged", logged.contains("151.2092955"))
        assertTrue("sibling parameters must not be redacted: $logged", logged.contains("method=3"))
        assertTrue("sibling parameters must not be redacted: $logged", logged.contains("school=0"))
    }

    @Test
    fun `an uppercase parameter name is still redacted`() {
        val logged = logFor(
            "https://api.aladhan.com/v1/timings/24-09-2026?Latitude=48.8566&Longitude=2.3522",
        )

        assertFalse("latitude leaked into the log: $logged", logged.contains("48.8566"))
        assertFalse("longitude leaked into the log: $logged", logged.contains("2.3522"))
    }

    @Test
    fun `the enabled interceptor runs at BASIC level`() {
        val interceptor = NetworkModule.createLoggingInterceptor()

        assertEquals(HttpLoggingInterceptor.Level.BASIC, interceptor.level)
    }

    @Test
    fun `BASIC level logs no request headers at all`() {
        val logged = logFor(
            prayerTimingsUrl,
            headers = mapOf("Authorization" to "Bearer super-secret-token", "Accept" to "application/json"),
        )

        // BASIC emits only the request line and the status line, so neither
        // credentials nor any other header can reach the log from this path.
        assertFalse("a header should not be logged at BASIC level: $logged", logged.contains("Authorization"))
        assertFalse("a header should not be logged at BASIC level: $logged", logged.contains("Accept:"))
    }

    @Test
    fun `credential headers are redacted when headers are logged`() {
        val logged = logFor(
            prayerTimingsUrl,
            headers = mapOf(
                "Authorization" to "Bearer super-secret-token",
                "Cookie" to "session=super-secret-cookie",
                "Proxy-Authorization" to "Basic super-secret-proxy",
                "Accept" to "application/json",
            ),
            level = HttpLoggingInterceptor.Level.HEADERS,
        )

        assertFalse("authorization header leaked: $logged", logged.contains("super-secret-token"))
        assertFalse("cookie leaked: $logged", logged.contains("super-secret-cookie"))
        assertFalse("proxy-authorization leaked: $logged", logged.contains("super-secret-proxy"))
        assertFalse("set-cookie leaked: $logged", logged.contains("super-secret-set-cookie"))

        // The header dump must still be happening, otherwise the assertions above
        // would pass for the wrong reason.
        assertTrue("headers should be logged at HEADERS level: $logged", logged.contains("Accept: application/json"))
    }

    @Test
    fun `response headers are not logged at BASIC level`() {
        val logged = logFor(prayerTimingsUrl, headers = mapOf("Accept" to "application/json"))

        assertFalse("a response body should not be logged: $logged", logged.contains("{}"))
        assertTrue("the status line should be logged: $logged", logged.contains("200"))
    }

    @Test
    fun `the scrubber is a no-op on a message without location parameters`() {
        val message = "--> GET https://api.aladhan.com/v1/timings/24-09-2026?method=3"

        assertEquals(message, NetworkModule.scrubSensitiveQuery(message))
    }
}

