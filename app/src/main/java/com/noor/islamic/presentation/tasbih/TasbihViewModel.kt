package com.noor.islamic.presentation.tasbih

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.domain.repository.TasbihRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The screen state: the live session plus the presets offered as targets. */
data class TasbihUiState(
    val session: TasbihSession = TasbihSession.Empty,
) {
    val count: Int get() = session.count
    val target: Int get() = session.target
    val remaining: Int get() = session.remaining
    val isComplete: Boolean get() = session.isComplete
    val presets: List<Int> get() = TasbihSession.PRESETS
    val canDecrement: Boolean get() = session.count > 0
    val fraction: Float
        get() = if (session.target <= 0) 0f
        else (session.count.toFloat() / session.target).coerceIn(0f, 1f)
}

/**
 * The Tasbih counter.
 *
 * Every action is a single clamped transition on top of the stored session, then
 * persisted, so the count can never go negative, overshoot the target, or drift
 * away from what is on disk after a restart. Undo decrements by one rather than
 * wiping the round, and a target change keeps the progress already made.
 *
 * Each transition is serialised by [mutex] and applied to [sessionState] *before*
 * the write, so several taps in quick succession are counted one by one instead of
 * all reading the same starting value. The stored session only seeds the state
 * while the user has not acted yet, so a tap that beats the initial read is never
 * overwritten by it.
 */
class TasbihViewModel(
    private val repository: TasbihRepository,
) : ViewModel() {

    private val mutex = Mutex()
    private val sessionState = MutableStateFlow<TasbihSession?>(null)

    val uiState: StateFlow<TasbihUiState> = sessionState
        .map { TasbihUiState(it ?: TasbihSession.Empty) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TasbihUiState(),
        )

    init {
        viewModelScope.launch {
            repository.observeSession().collect { stored ->
                // Only the first stored value seeds the state; afterwards the
                // repository is a write-through sink, so the screen owns the truth.
                sessionState.compareAndSet(expect = null, update = stored)
            }
        }
    }

    /** Adds one repetition. Stops at the target and stops counting after that. */
    fun increment() = mutate { session ->
        val next = (session.count + 1).coerceAtMost(session.target)
        session.copy(
            count = next,
            // A tap that cannot advance is not a repetition, so it must not
            // inflate the lifetime total.
            totalCount = if (next > session.count) session.totalCount + 1 else session.totalCount,
        )
    }

    /** Removes one repetition. Does nothing at zero. */
    fun decrement() = mutate { session ->
        session.copy(count = (session.count - 1).coerceAtLeast(0))
    }

    /** Starts the round again, keeping the lifetime total. */
    fun reset() = mutate { session -> session.copy(count = 0) }

    /**
     * Switches target. Progress already made is kept and clamped to the new
     * target, so raising a target never loses count and lowering it never
     * leaves the counter above the target.
     */
    fun setTarget(target: Int) = mutate { session ->
        val safeTarget = target.coerceIn(TasbihSession.MIN_TARGET, TasbihSession.MAX_TARGET)
        session.copy(
            target = safeTarget,
            count = session.count.coerceIn(0, safeTarget),
        )
    }

    private fun mutate(transform: (TasbihSession) -> TasbihSession) {
        viewModelScope.launch {
            mutex.withLock {
                val current = sessionState.value ?: TasbihSession.Empty
                val transformed = transform(current)
                val next = TasbihSession.sanitized(
                    count = transformed.count,
                    target = transformed.target,
                    totalCount = transformed.totalCount,
                )
                if (next == current) return@withLock
                sessionState.value = next
                repository.save(next)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
