package com.noor.islamic.presentation.athkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.athkar.CategoryProgress
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.athkar.DhikrProgress
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.AthkarProgressSnapshot
import com.noor.islamic.domain.repository.AthkarProvenance
import com.noor.islamic.domain.repository.DhikrRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** A category plus how much of it the user has completed. */
data class AthkarCategoryItem(
    val category: DhikrCategory,
    val progress: CategoryProgress,
)

sealed interface AthkarUiState {
    data object Loading : AthkarUiState

    /** Categories with bundled adhkar, and how far the user has got. */
    data class Ready(
        val items: List<AthkarCategoryItem>,
        val provenance: AthkarProvenance,
    ) : AthkarUiState

    /**
     * The reading pipeline works but no adhkar content has been cleared for
     * bundling. Shown instead of inventing categories or text.
     */
    data class NoContentBundled(val provenance: AthkarProvenance) : AthkarUiState

    data object Error : AthkarUiState
}

/**
 * Lists the adhkar categories available offline.
 *
 * The corpus is read-only bundled content, so it is loaded once; only the
 * repetition counters are observed reactively, which keeps returning to the
 * screen (or reopening the app) in step with the user's progress.
 */
class AthkarViewModel(
    private val dhikrRepository: DhikrRepository,
    private val progressRepository: AthkarProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AthkarUiState>(AthkarUiState.Loading)
    val uiState: StateFlow<AthkarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val corpus = loadCorpus()
                combine(
                    flowOf(corpus),
                    progressRepository.observeProgress(),
                ) { loaded, progress -> loaded to progress }
                    .collect { (loaded, progress) -> emit(loaded, progress) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = AthkarUiState.Error
            }
        }
    }

    /** Returns a reset action that clears every recorded dhikr repetition. */
    fun resetAll() {
        viewModelScope.launch { progressRepository.resetAll() }
    }

    private suspend fun loadCorpus(): CorpusView {
        val categories = dhikrRepository.categories()
        return CorpusView(
            provenance = dhikrRepository.provenanceSummary(),
            categories = categories,
            dhikrByCategory = categories.associate { category ->
                category.id to dhikrRepository.dhikrIn(category.id)
            },
        )
    }

    private fun emit(corpus: CorpusView, progress: AthkarProgressSnapshot) {
        if (corpus.categories.isEmpty() || corpus.dhikrByCategory.values.all { it.isEmpty() }) {
            _uiState.value = AthkarUiState.NoContentBundled(corpus.provenance)
            return
        }

        val items = corpus.categories
            .map { category ->
                val dhikr = corpus.dhikrByCategory[category.id].orEmpty()
                AthkarCategoryItem(
                    category = category,
                    progress = CategoryProgress(
                        categoryId = category.id,
                        totalDhikr = dhikr.size,
                        completedDhikr = dhikr.count { isComplete(it, progress) },
                        prescribedDhikr = dhikr.count { DhikrProgress.targetOf(it.repeatCount) != null },
                    ),
                )
            }
            .filter { it.progress.totalDhikr > 0 }

        _uiState.value = if (items.isEmpty()) {
            AthkarUiState.NoContentBundled(corpus.provenance)
        } else {
            AthkarUiState.Ready(items, corpus.provenance)
        }
    }

    private data class CorpusView(
        val provenance: AthkarProvenance,
        val categories: List<DhikrCategory>,
        val dhikrByCategory: Map<String, List<Dhikr>>,
    )

    private companion object {

        /**
         * Shared with the Home summary so both screens agree on what "finished"
         * means: only a dhikr with a prescribed count can ever be complete.
         */
        fun isComplete(dhikr: Dhikr, progress: AthkarProgressSnapshot): Boolean =
            DhikrProgress.isComplete(dhikr.repeatCount, progress.repetitionsOf(dhikr.id))
    }
}
