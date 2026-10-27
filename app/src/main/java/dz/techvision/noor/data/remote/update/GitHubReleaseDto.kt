package dz.techvision.noor.data.remote.update

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("draft") val draft: Boolean?,
    @SerializedName("prerelease") val prerelease: Boolean?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("assets") val assets: List<GitHubAssetDto>?,
)

data class GitHubAssetDto(
    @SerializedName("name") val name: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("browser_download_url") val browserDownloadUrl: String?,
    @SerializedName("content_type") val contentType: String?,
)

