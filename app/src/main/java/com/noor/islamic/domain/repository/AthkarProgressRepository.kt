package com.noor.islamic.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repetition progress for the adhkar the user has opened.
 *
 * Progress is stored per stable dhikr id, never per text, so it survives
 * reordering and corpus revisions. Only ids and counters are persisted; the
 * dhikr text always comes from the corpus.
 */
interface AthkarProgressRepository {

    /** Completed repetition count per dhikr id, for the ids the user has touched. */
    fun observeProgress(): Flow<AthkarProgressSnapshot>

    /**
     * Records progress for a dhikr after a repetition.
     *
     * Implementations clamp [completedRepetitions] to the dhikr's own target
     * (supplied by the caller) so a stored value can never exceed what the
     * corpus asks for.
     */
    suspend fun record(dhikrId: String, completedRepetitions: Int, target: Int)

    /** Clears the progress of a single dhikr. */
    suspend fun resetDhikr(dhikrId: String)

    /** Clears every dhikr recorded for the given category. */
    suspend fun resetCategory(categoryId: String, dhikrIds: List<String>)

    /** Clears all recorded adhkar progress. */
    suspend fun resetAll()
}

/** Immutable view of the recorded progress, safe to hand to the UI. */
data class AthkarProgressSnapshot(
    val repetitionsByDhikrId: Map<String, Int> = emptyMap(),
) {
    fun repetitionsOf(dhikrId: String): Int = repetitionsByDhikrId[dhikrId] ?: 0
}
