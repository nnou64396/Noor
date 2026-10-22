package com.noor.islamic.data.remote.update

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit API interface for fetching the remote update manifest.
 */
interface UpdateApi {
    @GET
    suspend fun fetchManifest(@Url manifestUrl: String): Response<UpdateManifestDto>
}
