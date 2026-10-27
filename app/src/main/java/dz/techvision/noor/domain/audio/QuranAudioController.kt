package dz.techvision.noor.domain.audio

import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.QuranAudioPlaybackState
import dz.techvision.noor.domain.model.quran.Reciter
import kotlinx.coroutines.flow.StateFlow

/**
 * A recitation queue: the ayahs of the surah currently open in the reader, in
 * canonical order. Audio never reaches outside the reader's own surah.
 */
data class QuranAudioQueue(
    val reciter: Reciter,
    val ayahs: List<AyahRef>,
    val startIndex: Int,
) {
    val isValid: Boolean
        get() = ayahs.isNotEmpty() && startIndex in ayahs.indices
}

/**
 * Playback control surface used by the Quran presentation layer.
 *
 * A single application-scoped implementation is shared by every screen, so
 * navigating between the reader, search, bookmarks and Home never duplicates a
 * player or loses the playback position.
 */
interface QuranAudioController {

    val state: StateFlow<QuranAudioPlaybackState>

    val reciters: List<Reciter>

    /** Starts (or restarts) the queue, optionally from a stored position. */
    fun play(queue: QuranAudioQueue, startPositionMs: Long = 0L)

    fun pause()

    fun resume()

    /** Stops playback and releases buffered media, keeping the stored resume point. */
    fun stop()

    fun skipToNext()

    fun skipToPrevious()

    fun seekTo(positionMs: Long)

    fun setRepeatCurrentSurah(enabled: Boolean)

    /** Persists the selection and, when playing, re-queues with the new reciter. */
    fun selectReciter(reciter: Reciter)

    fun release()
}

