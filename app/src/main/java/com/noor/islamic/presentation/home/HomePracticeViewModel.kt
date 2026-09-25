package com.noor.islamic.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.DhikrRepository
import com.noor.islamic.domain.repository.TasbihRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * A compact Phase 5 summary for Home: how many adhkar are finished and where the
 * Tasbih count stands. Null until both sources have answered, so Home can simply
 * skip the card instead of showing a half-loaded figure.
 */
data class HomePracticeUiState(
    val completedDhikr: Int = 0,
    val totalDhikr: Int = 0,
    val tasbih: TasbihSession = TasbihSession.Empty,
) {
    val hasContent: Boolean get() = totalDhikr > 0
}

/** Reads Athkar and Tasbih state for the Home card. Read-only, like every other Home source. */
class HomePracticeViewModel(
    private val dhikrRepository: DhikrRepository,
    private val progressRepository: AthkarProgressRepository,
    private val tasbihRepository: TasbihRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomePracticeUiState?>(null)
    val uiState: StateFlow<HomePracticeUiState?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val dhikr = dhikrRepository.allDhikr()
                combine(
                    progressRepository.observeProgress(),
                    tasbihRepository.observeSession(),
                ) { progress, tasbih -> progress to tasbih }
                    .collect { (progress, tasbih) ->
                        _uiState.value = HomePracticeUiState(
                            completedDhikr = dhikr.count { item ->
                                progress.repetitionsOf(item.id) >= item.repeatCount
                            },
                            totalDhikr = dhikr.size,
                            tasbih = tasbih,
                        )
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Home is a dashboard: a failed practice source simply shows no card.
                _uiState.value = null
            }
        }
    }
}
