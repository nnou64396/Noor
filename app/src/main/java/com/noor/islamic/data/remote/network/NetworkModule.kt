package com.noor.islamic.data.remote.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Network foundation using Retrofit + OkHttp.
 *
 * No API is consumed yet; remote features (Quran text, timings, ...)
 * will be connected in later phases.
 */
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L

    fun provideOkHttpClient(logging: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (logging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
    }

    fun provideRetrofit(
        baseUrl: String,
        client: OkHttpClient = provideOkHttpClient(),
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
}