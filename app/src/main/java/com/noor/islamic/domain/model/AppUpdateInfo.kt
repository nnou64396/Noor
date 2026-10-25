package com.noor.islamic.domain.model

/**
 * Validated domain model for application update details.
 */
data class AppUpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Long,
    val minimumSupportedVersionCode: Long,
    val mandatory: Boolean = false,
    val sha256: String? = null,
    val releaseNotes: Map<String, String> = emptyMap(),
    val downloadUrl: String? = null,
    val publishedAt: String? = null,
) {
    fun getReleaseNoteForLanguage(languageCode: String): String? =
        releaseNotes[languageCode]
            ?: releaseNotes["en"]
            ?: releaseNotes.values.firstOrNull()

    val isValid: Boolean
        get() = latestVersionCode > 0 && latestVersion.isNotBlank()
}
