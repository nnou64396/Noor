package dz.techvision.noor.data.remote.prayer

import dz.techvision.noor.data.remote.network.NetworkModule
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds the AlAdhan Retrofit service. Public endpoint, HTTPS only, no keys.
 */
object PrayerApiProvider {

    private const val BASE_URL = "https://api.aladhan.com/v1/"

    fun provide(client: OkHttpClient = NetworkModule.provideOkHttpClient()): PrayerApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerApi::class.java)
}
