package dz.techvision.noor.data.audio

import dz.techvision.noor.domain.audio.QuranAudioController
import dz.techvision.noor.domain.audio.QuranAudioQueue
import dz.techvision.noor.domain.model.quran.AyahAudioPosition
import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.QuranAudioError
import dz.techvision.noor.domain.model.quran.QuranAudioPlaybackState
import dz.techvision.noor.domain.model.quran.Reciter
import dz.techvision.noor.domain.repository.QuranAudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Playback state machine shared by the whole app (one instance, created by the
 * service locator).
 *
 * Responsibilities: build the per-ayah queue for the surah in the reader, translate
 * engine callbacks into [QuranAudioPlaybackState], advance between ayahs, honour
 * repeat, and persist a lightweight resume point. Failures are surfaced in the state
 * and never thrown, so the reader stays usable.
 */
class QuranAudioControllerImpl(
    private val repository: QuranAudioRepository,
    private val engine: AudioEngine,
    private val scope: CoroutineScope,
    private val catalog: ReciterCatalog = EveryAyahReciterCatalog,
    private val tickMillis: Long = TICK_MILLIS,
    private val persistIntervalMillis: Long = PERSIST_INTERVAL_MILLIS,
) : QuranAudioController {

    private val _state = MutableStateFlow(QuranAudioPlaybackState(reciter = catalog.defaultReciter))
    override val state: StateFlow<QuranAudioPlaybackState> = _state.asStateFlow()

    override val reciters: List<Reciter> get() = repository.reciters

    private var queue: QuranAudioQueue? = null
    private var isSyncing = false
    private var persistJob: Job? = null
    private var lastPersistedRef: AyahRef? = null
    private var lastPersistedPositionMs = 0L

    init {
        engine.setListener { syncFromEngine() }
        scope.launch {
            repository.selectedReciter.first()?.let { selected ->
                _state.value = _state.value.copy(reciter = selected)
            }
        }
        scope.launch {
            repository.resumePosition.first()?.let { stored ->
                _state.value = _state.value.copy(resumePoint = stored)
            }
        }
        scope.launch {
            repository.repeatCurrentSurah.collectLatest { enabled ->
                _state.value = _state.value.copy(repeatCurrentSurah = enabled)
            }
        }
        scope.launch {
            while (isActive) {
                delay(tickMillis)
                tick()
            }
        }
    }

    override fun play(queue: QuranAudioQueue, startPositionMs: Long) {
        if (!queue.isValid) return
        val sameQueue = this.queue == queue
        this.queue = queue

        if (!sameQueue) {
            engine.setQueue(queue.ayahs.map { catalog.itemFor(queue.reciter, it) })
            engine.prepare()
        }
        val sameItem = sameQueue && engine.currentIndex == queue.startIndex
        if (sameItem && !engine.isPlaying) {
            engine.play()
        } else {
            engine.seekTo(queue.startIndex, startPositionMs)
            engine.play()
        }
        _state.value = _state.value.copy(
            reciter = queue.reciter,
            error = null,
        )
        syncFromEngine()
    }

    override fun pause() {
        persistPosition(_state.value.currentAyah, _state.value.positionMs)
        engine.pause()
        syncFromEngine()
    }

    override fun resume() {
        if (_state.value.currentAyah == null) return
        engine.play()
        syncFromEngine()
    }

    override fun stop() {
        val current = _state.value
        persistPosition(current.currentAyah, current.positionMs)
        queue = null
        engine.stopAndClear()
        // The resume point survives stopping: it is what "continue" restores from.
        _state.value = QuranAudioPlaybackState(
            reciter = current.reciter,
            repeatCurrentSurah = current.repeatCurrentSurah,
            resumePoint = _state.value.resumePoint,
        )
    }

    override fun skipToNext() {
        val active = queue ?: return
        val nextIndex = engine.currentIndex + 1
        when {
            nextIndex <= active.ayahs.lastIndex -> engine.seekTo(nextIndex, 0L)
            _state.value.repeatCurrentSurah -> engine.seekTo(0, 0L)
            else -> stop()
        }
        syncFromEngine()
    }

    override fun skipToPrevious() {
        val active = queue ?: return
        // Restart the current ayah when already a few seconds in, like a music player.
        val target = if (engine.positionMs > RESTART_THRESHOLD_MS) {
            engine.currentIndex
        } else {
            engine.currentIndex - 1
        }
        engine.seekTo(target.coerceAtLeast(0), 0L)
        syncFromEngine()
    }

    override fun seekTo(positionMs: Long) {
        if (_state.value.currentAyah == null) return
        engine.seekTo(engine.currentIndex, positionMs)
        syncFromEngine()
    }

    override fun setRepeatCurrentSurah(enabled: Boolean) {
        _state.value = _state.value.copy(repeatCurrentSurah = enabled)
        scope.launch { repository.setRepeatCurrentSurah(enabled) }
    }

    override fun selectReciter(reciter: Reciter) {
        if (reciter.id == _state.value.reciter.id) return
        scope.launch { repository.selectReciter(reciter) }
        val active = queue
        val index = engine.currentIndex.coerceAtLeast(0)
        val position = _state.value.positionMs
        if (active != null) {
            val updated = active.copy(reciter = reciter)
            queue = updated
            engine.setQueue(updated.ayahs.map { catalog.itemFor(reciter, it) })
            engine.prepare()
            engine.seekTo(index, position)
            if (engine.isPlaying) engine.play()
        }
        _state.value = _state.value.copy(reciter = reciter, error = null)
        syncFromEngine()
    }

    override fun release() {
        stop()
        engine.setListener(null)
        engine.release()
    }

    /** Polls position while playing and persists at a bounded rate. */
    private fun tick() {
        if (!engine.isPlaying) return
        val current = _state.value
        val ref = current.currentAyah ?: return
        val position = engine.positionMs
        if (position != current.positionMs) {
            _state.value = current.copy(positionMs = position)
        }
        if (ref != lastPersistedRef ||
            position - lastPersistedPositionMs >= persistIntervalMillis
        ) {
            persistPosition(ref, position)
        }
    }

    /** Applies engine changes to the exposed state; re-entrant calls are ignored. */
    private fun syncFromEngine() {
        if (isSyncing) return
        isSyncing = true
        try {
            val active = queue
            if (active == null || active.ayahs.isEmpty()) {
                _state.value = _state.value.copy(
                    currentAyah = null,
                    isPlaying = false,
                    isBuffering = false,
                    positionMs = 0L,
                    durationMs = 0L,
                )
                return
            }
            if (engine.state == AudioEngineState.ENDED) {
                if (_state.value.repeatCurrentSurah) {
                    engine.seekTo(0, 0L)
                    engine.play()
                } else {
                    persistPosition(active.ayahs.last(), engine.positionMs)
                    queue = null
                    engine.stopAndClear()
                    _state.value = QuranAudioPlaybackState(
                        reciter = _state.value.reciter,
                        repeatCurrentSurah = _state.value.repeatCurrentSurah,
                    )
                    return
                }
            }
            val index = engine.currentIndex.coerceIn(0, active.ayahs.lastIndex)
            val ref = active.ayahs[index]
            val failure = engine.failure
            if (failure != null) {
                persistPosition(ref, engine.positionMs)
            }
            _state.value = _state.value.copy(
                currentAyah = ref,
                isPlaying = engine.isPlaying,
                isBuffering = engine.state == AudioEngineState.BUFFERING,
                positionMs = engine.positionMs,
                durationMs = engine.durationMs,
                error = failure?.let {
                    when (it) {
                        AudioEngineFailure.NetworkUnavailable -> QuranAudioError.NETWORK
                        AudioEngineFailure.SourceUnavailable -> QuranAudioError.SOURCE_UNAVAILABLE
                        AudioEngineFailure.Unknown -> QuranAudioError.UNKNOWN
                    }
                },
            )
        } finally {
            isSyncing = false
        }
    }

    private fun persistPosition(ref: AyahRef?, positionMs: Long) {
        if (ref == null) return
        lastPersistedRef = ref
        lastPersistedPositionMs = positionMs
        val snapshot = AyahAudioPosition(ref = ref, positionMs = positionMs)
        _state.value = _state.value.copy(resumePoint = snapshot)
        persistJob?.cancel()
        persistJob = scope.launch {
            repository.saveResumePosition(snapshot)
        }
    }

    private companion object {
        const val TICK_MILLIS = 500L
        const val PERSIST_INTERVAL_MILLIS = 5_000L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}

