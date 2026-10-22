package com.noor.islamic.data.remote.update

import com.google.gson.annotations.SerializedName

/**
 * Remote update manifest DTO structure for Gson parsing.
 */
data class UpdateManifestDto(
    @SerializedName("latestVersion") val latestVersion: String? = null,
    @SerializedName("latestVersionCode") val latestVersionCode: Long? = null,
    @SerializedName("minimumSupportedVersionCode") val minimumSupportedVersionCode: Long? = null,
    @SerializedName("releaseNotes") val releaseNotes: Map<String, String>? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("publishedAt") val publishedAt: String? = null,
)
