package dz.techvision.noor.data.remote.update

import com.google.gson.annotations.SerializedName

/**
 * Remote update manifest DTO structure supporting both new and legacy schemas.
 */
data class UpdateManifestDto(
    @SerializedName("schemaVersion") val schemaVersion: Int? = null,
    @SerializedName("versionName") val versionName: String? = null,
    @SerializedName("versionCode") val versionCode: Long? = null,
    @SerializedName("minimumSupportedVersionCode") val minimumSupportedVersionCode: Long? = null,
    @SerializedName("mandatory") val mandatory: Boolean? = null,
    @SerializedName("apkAsset") val apkAsset: String? = null,
    @SerializedName("sha256") val sha256: String? = null,
    @SerializedName("releaseNotes") val releaseNotes: Map<String, String>? = null,
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("latestVersion") val latestVersion: String? = null,
    @SerializedName("latestVersionCode") val latestVersionCode: Long? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
)

