package dz.techvision.noor.domain.model.quran

/**
 * Non-fatal audio problems. The Quran reader never fails because of these; it keeps
 * the text usable and only surfaces the audio problem.
 */
enum class QuranAudioError {
    NETWORK,
    SOURCE_UNAVAILABLE,
    UNKNOWN,
}

/** Last locally known playback position, used to resume where the user stopped. */
data class AyahAudioPosition(
    val ref: AyahRef,
    val positionMs: Long,
)

/** Immutable snapshot of Quran audio playback, observed by the UI. */
data class QuranAudioPlaybackState(
    val reciter: Reciter,
    val currentAyah: AyahRef? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatCurrentSurah: Boolean = false,
    val error: QuranAudioError? = null,
    val resumePoint: AyahAudioPosition? = null,
) {
    /** True when a queue is loaded, i.e. the mini player has something to show. */
    val hasQueue: Boolean get() = currentAyah != null

    /** True when the player is audible, buffering, or holds a meaningful position. */
    val isActive: Boolean
        get() = currentAyah != null && (isPlaying || isBuffering || positionMs > 0L)
}

