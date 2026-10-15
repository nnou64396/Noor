package com.noor.islamic.data.remote.network

import android.content.pm.ApplicationInfo
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Network foundation using Retrofit + OkHttp.
 *
 * HTTP logging is off by default and is only ever enabled for a debuggable
 * build. Even when enabled it runs at [HttpLoggingInterceptor.Level.BASIC] and
 * redacts the location query parameters and the standard credential headers, so
 * a precise latitude/longitude or an auth token can never reach Logcat.
 */
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val LOG_TAG = "NoorHttp"

    /**
     * Query parameters that carry the user's precise location in the prayer
     * timings request.
     */
    private val REDACTED_QUERY_PARAMS = listOf("latitude", "longitude")

    private val REDACTED_HEADERS = arrayOf("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization")

    /**
     * OkHttp 4.12 can redact header names but has no query-parameter redaction
     * (that arrived in OkHttp 5), so the URL is scrubbed as the message is
     * written. Matching on the parameter *name* rather than the value means a
     * change in coordinate precision can never slip past this.
     */
    private val QUERY_REDACTION = Regex(
        "((?:${REDACTED_QUERY_PARAMS.joinToString("|")})=)[^&\\s]*",
        RegexOption.IGNORE_CASE,
    )

    /** Stand-in written in place of a redacted value. */
    private const val REDACTED = "██"

    private val LOGCAT: HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger { message -> Log.d(LOG_TAG, message) }

    /**
     * True only for a debuggable build. Uses the platform flag rather than
     * `BuildConfig.DEBUG` so no build-configuration change is required.
     */
    fun shouldLogHttp(applicationInfoFlags: Int): Boolean =
        applicationInfoFlags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun provideOkHttpClient(logging: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (logging) {
            builder.addInterceptor(createLoggingInterceptor())
        }

        return builder.build()
    }

    /**
     * The single logging interceptor used by the app.
     *
     * OkHttp redacts the credential headers itself; the URL is scrubbed by
     * [scrubSensitiveQuery] on the way to the sink, which is the last point
     * before the message would be written to Logcat. [sink] exists so tests can
     * observe exactly what would have been logged.
     */
    fun createLoggingInterceptor(
        sink: HttpLoggingInterceptor.Logger = LOGCAT,
    ): HttpLoggingInterceptor =
        HttpLoggingInterceptor(
            HttpLoggingInterceptor.Logger { message -> sink.log(scrubSensitiveQuery(message)) },
        ).apply {
            level = HttpLoggingInterceptor.Level.BASIC
            for (header in REDACTED_HEADERS) {
                redactHeader(header)
            }
        }

    /**
     * Replaces the value of every location query parameter with [REDACTED],
     * keeping the parameter name so the log remains useful. Also covers the
     * request-line and header-dump forms OkHttp can emit.
     */
    internal fun scrubSensitiveQuery(message: String): String =
        QUERY_REDACTION.replace(message) { match -> "${match.groupValues[1]}$REDACTED" }

    fun provideRetrofit(
        baseUrl: String,
        client: OkHttpClient = provideOkHttpClient(),
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
}
