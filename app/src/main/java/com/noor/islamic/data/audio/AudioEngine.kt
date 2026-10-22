package com.noor.islamic.data.audio

/**
 * Thin, Media3-free view of a media player. Keeping playback behind this interface
 * lets the playback state machine be unit-tested on the JVM, and keeps Media3 types
 * out of the domain and presentation layers.
 */
interface AudioEngine {

    val state: AudioEngineState

    val isPlaying: Boolean

    val currentIndex: Int

    val positionMs: Long

    val durationMs: Long

    val failure: AudioEngineFailure?

    fun setQueue(items: List<AudioEngineItem>)

    fun prepare()

    fun play()

    fun pause()

    /** Stops playback and drops the media, releasing buffered bytes. */
    fun stopAndClear()

    fun seekTo(index: Int, positionMs: Long)

    fun setListener(listener: AudioEngineListener?)

    fun release()
}

enum class AudioEngineState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}

/** Engine-level failure already classified for the domain. */
sealed interface AudioEngineFailure {
    data object NetworkUnavailable : AudioEngineFailure
    data object SourceUnavailable : AudioEngineFailure
    data object Unknown : AudioEngineFailure
}

fun interface AudioEngineListener {
    fun onEngineChanged()
}

data class AudioEngineItem(
    val mediaId: String,
    val uri: String,
    /** Optional display line for the system media notification. */
    val title: String? = null,
)
