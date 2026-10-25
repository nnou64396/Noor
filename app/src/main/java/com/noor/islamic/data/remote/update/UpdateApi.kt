package com.noor.islamic.data.remote.update

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Retrofit API interface for fetching GitHub releases and update manifests.
 * Includes mandatory User-Agent header required by GitHub API specifications.
 */
interface UpdateApi {
    @Headers("User-Agent: Noor-Android")
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String = "nnou64396",
        @Path("repo") repo: String = "Noor",
    ): Response<List<GitHubReleaseDto>>

    @Headers("User-Agent: Noor-Android")
    @GET
    suspend fun fetchManifest(@Url manifestUrl: String): Response<UpdateManifestDto>
}
