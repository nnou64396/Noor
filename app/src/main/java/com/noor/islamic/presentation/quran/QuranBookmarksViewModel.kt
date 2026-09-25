package com.noor.islamic.presentation.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.quran.Bookmark
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranBookmarksRepository
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** A saved bookmark joined with its surah metadata for display. */
data class BookmarkItem(
    val bookmark: Bookmark,
    val surah: Surah?,
)

sealed interface QuranBookmarksUiState {
    data object Loading : QuranBookmarksUiState
    data class Ready(val items: List<BookmarkItem>) : QuranBookmarksUiState
    data object Empty : QuranBookmarksUiState
    data object Error : QuranBookmarksUiState
}

/** Lists locally saved bookmarks; no network and no account are involved. */
class QuranBookmarksViewModel(
    private val bookmarks: QuranBookmarksRepository,
    private val repository: QuranRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranBookmarksUiState>(QuranBookmarksUiState.Loading)
    val uiState: StateFlow<QuranBookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    bookmarks.observeBookmarks(),
                    repository.observeSurahs(),
                ) { saved, surahs -> saved to surahs.associateBy { it.surahNumber } }
                    .catch { throwable ->
                        if (throwable is CancellationException) throw throwable
                        _uiState.value = QuranBookmarksUiState.Error
                    }
                    .collect { (saved, surahs) -> emitState(saved, surahs) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = QuranBookmarksUiState.Error
            }
        }
    }

    fun remove(bookmark: Bookmark) {
        viewModelScope.launch { bookmarks.remove(bookmark.ref) }
    }

    private fun emitState(saved: List<Bookmark>, surahs: Map<Int, Surah>) {
        _uiState.value = if (saved.isEmpty()) {
            QuranBookmarksUiState.Empty
        } else {
            QuranBookmarksUiState.Ready(
                saved
                    .sortedByDescending { it.createdAt }
                    .map { BookmarkItem(it, surahs[it.ref.surahNumber]) },
            )
        }
    }
}
