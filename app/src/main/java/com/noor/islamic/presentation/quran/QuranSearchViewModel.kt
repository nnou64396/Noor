package com.noor.islamic.presentation.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * State of the offline Quran search screen.
 *
 * [Prompt] is the resting state, [Results] carries matches, and [Failed] is kept
 * separate from [Results] so a transient database error never looks like "no results".
 */
sealed interface QuranSearchUiState {
    data object Prompt : QuranSearchUiState
    data object Loading : QuranSearchUiState
    data class Results(val query: String, val results: List<Ayah>) : QuranSearchUiState
    data class Failed(val query: String) : QuranSearchUiState
}

/**
 * Searches the bundled FTS index with a debounced query. The index lives in the
 * immutable Quran database, so this adds no storage of its own and needs no network.
 */
@OptIn(FlowPreview::class)
class QuranSearchViewModel(
    private val repository: QuranRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<QuranSearchUiState>(QuranSearchUiState.Prompt)
    val uiState: StateFlow<QuranSearchUiState> = _uiState.asStateFlow()

    private val _surahs = MutableStateFlow<Map<Int, Surah>>(emptyMap())

    /** Surah lookup for result labels, so results never trigger one query per row. */
    val surahs: StateFlow<Map<Int, Surah>> = _surahs.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _surahs.value = repository.observeSurahs().first().associateBy { it.surahNumber }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Result labels fall back to the surah number when metadata is unavailable.
            }
        }
        _query
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { runSearch(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
        _uiState.value = QuranSearchUiState.Prompt
    }

    fun retry() {
        // Re-run the current term directly: the debounce would swallow a re-emit.
        runSearch(_query.value)
    }

    private fun runSearch(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = QuranSearchUiState.Prompt
            return
        }
        _uiState.value = QuranSearchUiState.Loading
        viewModelScope.launch {
            try {
                val results = repository.search(trimmed, SEARCH_RESULT_LIMIT)
                _uiState.value = QuranSearchUiState.Results(query = trimmed, results = results)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = QuranSearchUiState.Failed(query = trimmed)
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val SEARCH_RESULT_LIMIT = 50
    }
}
