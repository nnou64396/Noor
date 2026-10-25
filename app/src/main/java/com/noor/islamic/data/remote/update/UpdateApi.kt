package com.noor.islamic.data.remote.update

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit API interface for fetching GitHub releases and update manifests.
 */
interface UpdateApi {
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String = "nnou64396",
        @Path("repo") repo: String = "Noor",
    ): Response<List<GitHubReleaseDto>>

    @GET
    suspend fun fetchManifest(@Url manifestUrl: String): Response<UpdateManifestDto>
}
