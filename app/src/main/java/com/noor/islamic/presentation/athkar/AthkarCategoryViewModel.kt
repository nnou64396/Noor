package com.noor.islamic.presentation.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.AthkarProgressSnapshot
import com.noor.islamic.domain.repository.DhikrRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** One dhikr with the user's repetition progress against its required count. */
data class AthkarItemUi(
    val dhikr: Dhikr,
    val completedRepetitions: Int,
) {
    val remaining: Int get() = (dhikr.repeatCount - completedRepetitions).coerceAtLeast(0)
    val isComplete: Boolean get() = dhikr.repeatCount > 0 && completedRepetitions >= dhikr.repeatCount
    val fraction: Float
        get() = if (dhikr.repeatCount <= 0) 0f
        else (completedRepetitions.toFloat() / dhikr.repeatCount).coerceIn(0f, 1f)
}

sealed interface AthkarCategoryUiState {
    data object Loading : AthkarCategoryUiState

    data class Ready(
        val category: DhikrCategory,
        val items: List<AthkarItemUi>,
    ) : AthkarCategoryUiState {
        val completedCount: Int get() = items.count { it.isComplete }
        val totalCount: Int get() = items.size
        val isCategoryComplete: Boolean get() = items.isNotEmpty() && completedCount == totalCount
    }

    /** The slug is unknown, so nothing can be shown for it. */
    data object NotFound : AthkarCategoryUiState

    data object Error : AthkarCategoryUiState
}

/**
 * Drives one category of adhkar: repetition counting, completion and reset.
 *
 * The repetition count is clamped to the dhikr's own target before it is stored,
 * so a fast double-tap (or a restored value from an older corpus) can never push
 * a counter past what the source asks for.
 */
class AthkarCategoryViewModel(
    private val categoryId: String,
    private val dhikrRepository: DhikrRepository,
    private val progressRepository: AthkarProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AthkarCategoryUiState>(AthkarCategoryUiState.Loading)
    val uiState: StateFlow<AthkarCategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val dhikr = dhikrRepository.dhikrIn(categoryId)
                val category = dhikrRepository.categories().firstOrNull { it.id == categoryId }
                if (category == null || dhikr.isEmpty()) {
                    _uiState.value = AthkarCategoryUiState.NotFound
                    return@launch
                }
                combine(
                    flowOf(category to dhikr),
                    progressRepository.observeProgress(),
                ) { content, progress -> content to progress }
                    .collect { (content, progress) ->
                        val (loadedCategory, loadedDhikr) = content
                        _uiState.value = AthkarCategoryUiState.Ready(
                            category = loadedCategory,
                            items = loadedDhikr.map { it.toUi(progress) },
                        )
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = AthkarCategoryUiState.Error
            }
        }
    }

    /** Records one repetition. No-op once the dhikr is complete. */
    fun repeat(dhikr: Dhikr) {
        val current = (_uiState.value as? AthkarCategoryUiState.Ready)
            ?.items
            ?.firstOrNull { it.dhikr.id == dhikr.id }
            ?: return
        if (current.isComplete) return

        val next = (current.completedRepetitions + 1).coerceAtMost(dhikr.repeatCount)
        viewModelScope.launch {
            progressRepository.record(dhikr.id, next, dhikr.repeatCount)
        }
    }

    fun resetDhikr(dhikr: Dhikr) {
        viewModelScope.launch { progressRepository.resetDhikr(dhikr.id) }
    }

    fun resetCategory() {
        val state = _uiState.value as? AthkarCategoryUiState.Ready ?: return
        val ids = state.items.map { it.dhikr.id }
        viewModelScope.launch { progressRepository.resetCategory(state.category.id, ids) }
    }

    private fun Dhikr.toUi(progress: AthkarProgressSnapshot) = AthkarItemUi(
        dhikr = this,
        completedRepetitions = progress.repetitionsOf(id).coerceIn(0, repeatCount.coerceAtLeast(0)),
    )
}
