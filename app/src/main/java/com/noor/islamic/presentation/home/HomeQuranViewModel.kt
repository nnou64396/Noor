package com.noor.islamic.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.audio.QuranAudioController
import com.noor.islamic.domain.model.quran.QuranAudioPlaybackState
import com.noor.islamic.domain.model.quran.ReciterNameKey
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import com.noor.islamic.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Where "Continue reading" should jump to. */
data class ContinueReadingTarget(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val surah: Surah?,
)

/** Recitation currently loaded by the shared player. */
data class NowPlayingSummary(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val reciterNameKey: ReciterNameKey,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val playback: QuranAudioPlaybackState,
)

data class HomeQuranUiState(
    val continueReading: ContinueReadingTarget? = null,
    val nowPlaying: NowPlayingSummary? = null,
)

/**
 * Feeds the Home screen from the local reading position and the shared player.
 * Everything comes from the device (Quran database, app-private state store, audio
 * controller), so this screen needs no network call and no account.
 */
class HomeQuranViewModel(
    readingProgress: ReadingProgressRepository,
    repository: QuranRepository,
    private val audio: QuranAudioController?,
) : ViewModel() {

    val uiState: StateFlow<HomeQuranUiState> = combine(
        readingProgress.observeProgress(),
        repository.observeSurahs(),
        audio?.state ?: flowOf(null),
    ) { progress, surahs, playback ->
        val byNumber = surahs.associateBy { it.surahNumber }
        HomeQuranUiState(
            continueReading = progress?.let { saved ->
                ContinueReadingTarget(
                    surahNumber = saved.ref.surahNumber,
                    ayahNumberInSurah = saved.ref.ayahNumberInSurah,
                    surah = byNumber[saved.ref.surahNumber],
                )
            },
            nowPlaying = playback?.let { state ->
                state.currentAyah?.let { ref ->
                    NowPlayingSummary(
                        surahNumber = ref.surahNumber,
                        ayahNumberInSurah = ref.ayahNumberInSurah,
                        reciterNameKey = state.reciter.nameKey,
                        isPlaying = state.isPlaying,
                        isBuffering = state.isBuffering,
                        playback = state,
                    )
                }
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = HomeQuranUiState(),
    )

    fun togglePlayback() {
        val controller = audio ?: return
        if (controller.state.value.isPlaying) controller.pause() else controller.resume()
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
