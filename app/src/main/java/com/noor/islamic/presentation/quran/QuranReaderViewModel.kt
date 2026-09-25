package com.noor.islamic.presentation.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.quran.Ayah
import com.noor.islamic.domain.model.quran.Surah
import com.noor.islamic.domain.repository.QuranRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuranReaderUiState {
    data object Loading : QuranReaderUiState
    data class Ready(val surah: Surah, val ayahs: List<Ayah>) : QuranReaderUiState
    data object Empty : QuranReaderUiState
    data class Error(val kind: QuranReaderError) : QuranReaderUiState
}

enum class QuranReaderError {
    InvalidSurahNumber,
    SurahNotFound,
    NoAyahs,
    LoadFailed,
}

class QuranReaderViewModel(
    private val repository: QuranRepository,
    private val surahNumber: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Loading)
    val uiState: StateFlow<QuranReaderUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

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
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = QuranReaderUiState.Loading
            try {
                val surah = repository.getSurah(surahNumber)
                if (surah == null || surah.surahNumber != surahNumber) {
                    _uiState.value = QuranReaderUiState.Error(QuranReaderError.SurahNotFound)
                    return@launch
                }
                val ayahs = repository.getAyahs(surahNumber)
                _uiState.value = if (ayahs.isEmpty()) {
                    QuranReaderUiState.Empty
                } else {
                    QuranReaderUiState.Ready(surah, ayahs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = QuranReaderUiState.Error(QuranReaderError.LoadFailed)
            }
        }
    }
}
