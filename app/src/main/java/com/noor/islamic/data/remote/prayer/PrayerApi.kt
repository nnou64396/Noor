package com.noor.islamic.data.remote.prayer

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * AlAdhan public prayer-times API (https://api.aladhan.com/).
 * HTTPS only; the timings endpoint is open and needs no API key.
 */
interface PrayerApi {

    /**
     * Daily timings for a fixed `dd-MM-yyyy` [date] computed from
     * latitude/longitude with the chosen calculation [method] and
     * [school] (Fiqh standard for Asr).
     */
    @GET("timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int,
    ): TimingsResponse
}