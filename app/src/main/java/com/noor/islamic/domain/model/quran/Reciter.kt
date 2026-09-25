package com.noor.islamic.domain.model.quran

/**
 * Recitation style of a [Reciter]. Kept as a domain enum so no user-visible text
 * lives in code; the UI resolves localized labels from resources.
 */
enum class RecitationStyle {
    MURATTAL,
    MUJAWWAD,
    MUALLIM,
}

/**
 * Stable localization key for a reciter's name. Reciter names are proper nouns that
 * are still translated per app language, so they are resolved from resources rather
 * than hardcoded in Kotlin.
 */
enum class ReciterNameKey {
    ALAFASY,
    ABDUL_BASIT,
    HUSARY,
    MINSHAWY,
    AS_SUDAIS,
    AYYOUB,
    AD_DUSSARY,
}

/**
 * A Quran recitation available for streaming.
 *
 * Audio metadata is intentionally modeled outside the offline Quran text database:
 * [id] is the stable storage key and is also the folder name published by the audio
 * source, so switching reciters never touches the authoritative corpus.
 */
data class Reciter(
    val id: String,
    val nameKey: ReciterNameKey,
    val style: RecitationStyle,
)
