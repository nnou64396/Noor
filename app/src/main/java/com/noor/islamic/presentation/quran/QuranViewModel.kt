package com.noor.islamic.presentation.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface QuranUiState {
    data object Loading : QuranUiState
    data class Success(val surahs: List<Surah>) : QuranUiState
    data object Empty : QuranUiState
    data class Error(val kind: QuranIndexError) : QuranUiState
}

enum class QuranIndexError {
    LoadFailed,
}

class QuranViewModel(
    private val repository: QuranRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = QuranUiState.Loading
            try {
                val surahs = repository.observeSurahs().first()
                _uiState.value = if (surahs.isEmpty()) {
                    QuranUiState.Empty
                } else {
                    QuranUiState.Success(surahs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = QuranUiState.Error(QuranIndexError.LoadFailed)
            }
        }
    }
}
