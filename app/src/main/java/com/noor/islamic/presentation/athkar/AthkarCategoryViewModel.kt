package com.noor.islamic.presentation.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.athkar.DhikrProgress
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

/**
 * One dhikr with the user's repetition progress.
 *
 * Everything that needs a number is nullable and derived through
 * [DhikrProgress], so a dhikr with no prescribed count exposes no target: there
 * is no remaining count, no fraction to draw a progress bar from, and no
 * completion to announce. The counter itself is still real and still persisted.
 */
data class AthkarItemUi(
    val dhikr: Dhikr,
    val completedRepetitions: Int,
) {
    private val count get() = dhikr.repeatCount

    /** True only when the source prescribes a count, so a target may be shown. */
    val hasPrescribedCount: Boolean get() = DhikrProgress.targetOf(count) != null

    /** Repetitions still to do, or null when no count is prescribed. */
    val remaining: Int? get() = DhikrProgress.remainingOf(count, completedRepetitions)

    /** Never true for an unspecified count, however many times it is counted. */
    val isComplete: Boolean get() = DhikrProgress.isComplete(count, completedRepetitions)

    /** Null when there is no target, so no percentage can be derived. */
    val fraction: Float? get() = DhikrProgress.fractionOf(count, completedRepetitions)
}

sealed interface AthkarCategoryUiState {
    data object Loading : AthkarCategoryUiState

    data class Ready(
        val category: DhikrCategory,
        val items: List<AthkarItemUi>,
    ) : AthkarCategoryUiState {
        val totalCount: Int get() = items.size

        /** Items the corpus gives a repetition count to: the only ones that can complete. */
        val prescribedCount: Int get() = items.count { it.hasPrescribedCount }
        val completedCount: Int get() = items.count { it.isComplete }

        /**
         * False for a category whose adhkar all have unspecified counts. The UI
         * uses this to show an honest explanation rather than a 0-of-0 progress.
         */
        val hasPrescribedCounts: Boolean get() = prescribedCount > 0

        val isCategoryComplete: Boolean get() = hasPrescribedCounts && completedCount == prescribedCount
    }

    /** The slug is unknown, so nothing can be shown for it. */
    data object NotFound : AthkarCategoryUiState

    data object Error : AthkarCategoryUiState
}

/**
 * Drives one category of adhkar: repetition counting, completion and reset.
 *
 * A prescribed count is clamped to what the source asks for, so a fast
 * double-tap (or a restored value from an older corpus) can never push a counter
 * past the requirement. An unspecified count is counted freely and is never
 * treated as finished.
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

    /** Records one repetition. No-op once a dhikr with a prescribed count is complete. */
    fun repeat(dhikr: Dhikr) {
        val current = (_uiState.value as? AthkarCategoryUiState.Ready)
            ?.items
            ?.firstOrNull { it.dhikr.id == dhikr.id }
            ?: return
        if (current.isComplete) return

        val next = DhikrProgress.nextCount(dhikr.repeatCount, current.completedRepetitions)
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
        completedRepetitions = DhikrProgress.clampStored(repeatCount, progress.repetitionsOf(id)),
    )
}
