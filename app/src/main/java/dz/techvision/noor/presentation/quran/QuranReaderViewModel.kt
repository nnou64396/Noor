package dz.techvision.noor.presentation.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dz.techvision.noor.domain.audio.QuranAudioController
import dz.techvision.noor.domain.audio.QuranAudioQueue
import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.Bookmark
import dz.techvision.noor.domain.model.quran.QuranAudioPlaybackState
import dz.techvision.noor.domain.model.quran.ReadingProgress
import dz.techvision.noor.domain.model.quran.Reciter
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.domain.repository.QuranBookmarksRepository
import dz.techvision.noor.domain.repository.QuranRepository
import dz.techvision.noor.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

sealed interface QuranReaderUiState {
    data object Loading : QuranReaderUiState
    data class Ready(
        val surah: Surah,
        val ayahs: List<Ayah>,
        val currentIndex: Int = 0,
        val bookmarkedAyahNumbers: Set<Int> = emptySet(),
        val audio: QuranAudioPlaybackState? = null,
    ) : QuranReaderUiState
    data object Empty : QuranReaderUiState
    data class Error(val kind: QuranReaderError) : QuranReaderUiState
}

enum class QuranReaderError {
    InvalidSurahNumber,
    SurahNotFound,
    NoAyahs,
    LoadFailed,
}

/**
 * Reader state for one surah: the offline text plus the Phase 4 user state
 * (bookmarks, reading progress, recitation).
 *
 * The optional collaborators default to inert implementations so previews and
 * existing tests keep working with the original two-argument construction.
 */
class QuranReaderViewModel(
    private val repository: QuranRepository,
    private val surahNumber: Int,
    private val initialAyahNumber: Int? = null,
    private val bookmarks: QuranBookmarksRepository = InertQuranBookmarks,
    private val readingProgress: ReadingProgressRepository = InertReadingProgress,
    private val audio: QuranAudioController? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Loading)
    val uiState: StateFlow<QuranReaderUiState> = _uiState.asStateFlow()

    private val progressRequests = MutableSharedFlow<AyahRef>(extraBufferCapacity = 1)

    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var currentAyah: Ayah? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        if (surahNumber !in 1..114) {
            _uiState.value = QuranReaderUiState.Error(QuranReaderError.InvalidSurahNumber)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = QuranReaderUiState.Loading
            val surah: Surah
            val ayahs: List<Ayah>
            try {
                val loadedSurah = repository.getSurah(surahNumber)
                if (loadedSurah == null || loadedSurah.surahNumber != surahNumber) {
                    _uiState.value = QuranReaderUiState.Error(QuranReaderError.SurahNotFound)
                    return@launch
                }
                surah = loadedSurah
                ayahs = repository.getAyahs(surahNumber)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = QuranReaderUiState.Error(QuranReaderError.LoadFailed)
                return@launch
            }
            if (ayahs.isEmpty()) {
                _uiState.value = QuranReaderUiState.Empty
                return@launch
            }

            val startIndex = resolveStartIndex(ayahs)
            currentAyah = ayahs[startIndex]
            _uiState.value = QuranReaderUiState.Ready(
                surah = surah,
                ayahs = ayahs,
                currentIndex = startIndex,
            )
            observeExtras(this, ayahs)
        }
    }

    private suspend fun resolveStartIndex(ayahs: List<Ayah>): Int {
        initialAyahNumber?.let { requested ->
            val index = ayahs.indexOfFirst { it.ayahNumberInSurah == requested }
            if (index >= 0) return index
        }
        val stored = readingProgress.observeProgress().first()
        if (stored != null && stored.ref.surahNumber == surahNumber) {
            val index = ayahs.indexOfFirst { it.globalAyahNumber == stored.ref.globalAyahNumber }
            if (index >= 0) return index
        }
        return 0
    }

    /**
     * Bookmark/progress/audio followers are children of the load job, so a retry
     * replaces them instead of stacking duplicate collectors.
     */
    private fun observeExtras(scope: CoroutineScope, ayahs: List<Ayah>) {
        scope.launch {
            bookmarks.observeBookmarkedAyahNumbers(surahNumber).collectLatest { marked ->
                updateReady { ready -> ready.copy(bookmarkedAyahNumbers = marked) }
            }
        }
        audio?.let { controller ->
            scope.launch {
                controller.state.collectLatest { playback ->
                    updateReady { ready ->
                        val index = playback.currentAyah
                            ?.let { ref -> ayahs.indexOfFirst { it.globalAyahNumber == ref.globalAyahNumber } }
                            ?: -1
                        if (index >= 0) {
                            if (index != ready.currentIndex) {
                                currentAyah = ayahs[index]
                            }
                            ready.copy(audio = playback, currentIndex = index)
                        } else {
                            ready.copy(audio = playback)
                        }
                    }
                }
            }
        }
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            // Trailing debounce: scrolling the reader does not write on every ayah.
            progressRequests.collectLatest { ref ->
                delay(PROGRESS_DEBOUNCE_MS)
                readingProgress.saveProgress(ref)
            }
        }
    }

    /** Called when the user taps an ayah, or when scrolling settles on one. */
    fun selectAyah(index: Int) {
        val ready = _uiState.value as? QuranReaderUiState.Ready ?: return
        val ayah = ready.ayahs.getOrNull(index) ?: return
        if (index == ready.currentIndex && ayah == currentAyah) return
        currentAyah = ayah
        updateReady { it.copy(currentIndex = index) }
        requestProgressSave(ayah)
    }

    fun currentAyah(): Ayah? = currentAyah

    fun toggleBookmark() {
        val ayah = currentAyah ?: return
        toggleBookmarkOf(ayah)
    }

    /** Bookmarks the tapped ayah, whether or not it is the one being read. */
    fun toggleBookmarkOf(ayah: Ayah) {
        val ready = _uiState.value as? QuranReaderUiState.Ready ?: return
        viewModelScope.launch {
            if (ready.bookmarkedAyahNumbers.contains(ayah.ayahNumberInSurah)) {
                bookmarks.remove(AyahRef.of(ayah))
            } else {
                bookmarks.add(AyahRef.of(ayah))
            }
        }
    }

    /** Starts recitation at a specific ayah and makes it the reader's current ayah. */
    fun playAyah(ayah: Ayah) {
        val controller = audio ?: return
        val ready = _uiState.value as? QuranReaderUiState.Ready ?: return
        val index = ready.ayahs.indexOfFirst { it.globalAyahNumber == ayah.globalAyahNumber }
        if (index < 0) return
        val playback = controller.state.value
        if (playback.currentAyah?.globalAyahNumber == ayah.globalAyahNumber && playback.isPlaying) {
            controller.pause()
            return
        }
        if (index != ready.currentIndex) {
            currentAyah = ayah
            updateReady { it.copy(currentIndex = index) }
            requestProgressSave(ayah)
        }
        startPlayback(controller, ready, ayah)
    }

    /** Writes the reading position immediately (used when leaving the screen). */
    fun flushProgress() {
        val ayah = currentAyah ?: return
        viewModelScope.launch { readingProgress.saveProgress(AyahRef.of(ayah)) }
    }

    fun togglePlayback() {
        val controller = audio ?: return
        val ready = _uiState.value as? QuranReaderUiState.Ready ?: return
        val playback = controller.state.value
        val ayah = currentAyah ?: return
        val isThisAyah = playback.currentAyah?.globalAyahNumber == ayah.globalAyahNumber
        when {
            playback.isPlaying && isThisAyah -> controller.pause()
            isThisAyah -> controller.resume()
            else -> startPlayback(controller, ready, ayah)
        }
    }

    fun skipToNextAyah() {
        audio?.skipToNext()
    }

    fun skipToPreviousAyah() {
        audio?.skipToPrevious()
    }

    fun seekAudio(positionMs: Long) {
        audio?.seekTo(positionMs)
    }

    fun setRepeatCurrentSurah(enabled: Boolean) {
        audio?.setRepeatCurrentSurah(enabled)
    }

    fun selectReciter(reciter: Reciter) {
        audio?.selectReciter(reciter)
    }

    private fun startPlayback(
        controller: QuranAudioController,
        ready: QuranReaderUiState.Ready,
        ayah: Ayah,
    ) {
        val startIndex = ready.ayahs.indexOfFirst { it.globalAyahNumber == ayah.globalAyahNumber }
        if (startIndex < 0) return
        val resumePoint = controller.state.value.resumePoint
        val startPositionMs = if (resumePoint?.ref?.globalAyahNumber == ayah.globalAyahNumber) {
            resumePoint.positionMs
        } else {
            0L
        }
        controller.play(
            queue = QuranAudioQueue(
                reciter = controller.state.value.reciter,
                ayahs = ready.ayahs.map(AyahRef::of),
                startIndex = startIndex,
            ),
            startPositionMs = startPositionMs,
        )
    }

    private fun requestProgressSave(ayah: Ayah) {
        progressRequests.tryEmit(AyahRef.of(ayah))
    }

    private inline fun updateReady(transform: (QuranReaderUiState.Ready) -> QuranReaderUiState.Ready) {
        val current = _uiState.value
        if (current is QuranReaderUiState.Ready) {
            _uiState.value = transform(current)
        }
    }

    private companion object {
        const val PROGRESS_DEBOUNCE_MS = 700L
    }
}

private object InertQuranBookmarks : QuranBookmarksRepository {
    override fun observeBookmarks() = flowOf(emptyList<Bookmark>())
    override fun observeBookmarkedAyahNumbers(surahNumber: Int) = flowOf(emptySet<Int>())
    override suspend fun add(ref: AyahRef) = false
    override suspend fun remove(ref: AyahRef) = Unit
    override suspend fun isBookmarked(ref: AyahRef) = false
}

private object InertReadingProgress : ReadingProgressRepository {
    override fun observeProgress() = flowOf<ReadingProgress?>(null)
    override suspend fun saveProgress(ref: AyahRef) = Unit
    override suspend fun clearProgress() = Unit
}

