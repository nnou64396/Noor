package dz.techvision.noor.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dz.techvision.noor.domain.model.athkar.DhikrProgress
import dz.techvision.noor.domain.model.tasbih.TasbihSession
import dz.techvision.noor.domain.repository.AthkarProgressRepository
import dz.techvision.noor.domain.repository.DhikrRepository
import dz.techvision.noor.domain.repository.TasbihRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * A compact Phase 5 summary for Home: how many adhkar with a prescribed
 * repetition count are finished and where the Tasbih count stands. Null until
 * both sources have answered, so Home can simply skip the card instead of
 * showing a half-loaded figure.
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
                // Only dhikr with a prescribed count contribute to the Home card:
                // a dhikr with no count can never complete, so including it would
                // either inflate the total or force a completion figure that the
                // corpus never supported.
                val prescribed = dhikr.filter { DhikrProgress.targetOf(it.repeatCount) != null }
                combine(
                    progressRepository.observeProgress(),
                    tasbihRepository.observeSession(),
                ) { progress, tasbih -> progress to tasbih }
                    .collect { (progress, tasbih) ->
                        _uiState.value = HomePracticeUiState(
                            completedDhikr = prescribed.count { item ->
                                DhikrProgress.isComplete(
                                    item.repeatCount,
                                    progress.repetitionsOf(item.id),
                                )
                            },
                            totalDhikr = prescribed.size,
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

