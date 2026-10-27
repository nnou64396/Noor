package dz.techvision.noor.data.audio

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import dz.techvision.noor.domain.model.quran.QuranAudioError
import java.io.IOException

/**
 * Media3/ExoPlayer adapter. Translates player callbacks and errors into the
 * engine-agnostic [AudioEngine] contract; all playback policy lives in
 * `QuranAudioControllerImpl`.
 *
 * The player is app-scoped and shared: the service exposes it through a media
 * session while the in-app controller drives it directly, so both drive the
 * exact same playback position.
 */
class Media3AudioEngine(context: Context) : AudioEngine {

    private val appContext: Context = context.applicationContext

    /** Shared with [QuranMediaSessionService]; internal to this module on purpose. */
    internal val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private var listener: AudioEngineListener? = null

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) = notifyChanged()
                override fun onIsPlayingChanged(isPlaying: Boolean) = notifyChanged()
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = notifyChanged()
                override fun onPlayerError(error: PlaybackException) = notifyChanged()
            },
        )
    }

    override val state: AudioEngineState
        get() = when (player.playbackState) {
            Player.STATE_IDLE -> AudioEngineState.IDLE
            Player.STATE_BUFFERING -> AudioEngineState.BUFFERING
            Player.STATE_READY -> AudioEngineState.READY
            Player.STATE_ENDED -> AudioEngineState.ENDED
            else -> AudioEngineState.IDLE
        }

    override val isPlaying: Boolean get() = player.isPlaying

    override val currentIndex: Int get() = player.currentMediaItemIndex

    override val positionMs: Long get() = player.currentPosition.coerceAtLeast(0L)

    override val durationMs: Long
        get() = player.duration.takeIf { it > 0L && it != androidx.media3.common.C.TIME_UNSET } ?: 0L

    override val failure: AudioEngineFailure?
        get() = player.playerError?.toEngineFailure()

    override fun setQueue(items: List<AudioEngineItem>) {
        player.setMediaItems(
            items.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.mediaId)
                    .setUri(item.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(item.title).build(),
                    )
                    .build()
            },
        )
    }

    override fun prepare() {
        player.prepare()
    }

    override fun play() {
        // Bring the media session service up so system transport controls and
        // the media playback foreground service are available for this batch.
        startMediaSessionService()
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stopAndClear() {
        player.stop()
        player.clearMediaItems()
        // No queue, no playback: release the media service. The shared player
        // itself is untouched (the session releases only the session, never
        // the player).
        appContext.stopService(Intent(appContext, QuranMediaSessionService::class.java))
    }

    override fun seekTo(index: Int, positionMs: Long) {
        if (index < 0) return
        player.seekTo(index, positionMs.coerceAtLeast(0L))
    }

    override fun setListener(listener: AudioEngineListener?) {
        this.listener = listener
    }

    override fun release() {
        listener = null
        player.release()
    }

    private fun notifyChanged() {
        listener?.onEngineChanged()
    }

    private fun startMediaSessionService() {
        val intent = Intent(appContext, QuranMediaSessionService::class.java)
        runCatching { appContext.startForegroundService(intent) }
            .onFailure { appContext.startService(intent) }
    }

    private fun PlaybackException.toEngineFailure(): AudioEngineFailure {
        val chain: List<Throwable> = generateSequence<Throwable>(this) { it.cause }.toList()
        val invalidResponse = chain.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
        if (invalidResponse != null) {
            return when (invalidResponse.responseCode) {
                403, 404 -> AudioEngineFailure.SourceUnavailable
                else -> AudioEngineFailure.NetworkUnavailable
            }
        }
        return if (chain.any { it is IOException }) {
            AudioEngineFailure.NetworkUnavailable
        } else {
            AudioEngineFailure.Unknown
        }
    }
}

/** Maps an engine failure onto the domain error surfaced to the UI. */
fun AudioEngineFailure.toQuranAudioError(): QuranAudioError = when (this) {
    AudioEngineFailure.NetworkUnavailable -> QuranAudioError.NETWORK
    AudioEngineFailure.SourceUnavailable -> QuranAudioError.SOURCE_UNAVAILABLE
    AudioEngineFailure.Unknown -> QuranAudioError.UNKNOWN
}

