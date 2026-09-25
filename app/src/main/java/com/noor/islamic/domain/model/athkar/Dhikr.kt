package com.noor.islamic.domain.model.athkar

/**
 * One remembrance, as published by a curated, attributed corpus.
 *
 * [id] is the stable identity assigned by that corpus (never the position in a
 * list or file), so user progress keeps pointing at the same dhikr across
 * reordering and corpus updates. The Arabic in [arabicText] is reproduced
 * verbatim from the source; the app never rewrites it.
 */
data class Dhikr(
    val id: String,
    val categoryId: String,
    val arabicText: String,
    val translation: String? = null,
    val repeatCount: Int,
    val reference: String? = null,
    val order: Int,
) {
    val isRepeatable: Boolean get() = repeatCount > 1
}

/**
 * A group of adhkar, e.g. morning or before sleep.
 *
 * [id] is a stable slug (`morning`, `evening`, `after_prayer`, `before_sleep`,
 * `general`). Titles are resolved from Android resources rather than stored in
 * the corpus, so the bundled data stays language-neutral and every screen
 * remains localizable.
 */
data class DhikrCategory(
    val id: String,
    val order: Int,
    val titleResKey: String = id,
)

/** How much of a category the user has completed so far. */
data class CategoryProgress(
    val categoryId: String,
    val totalDhikr: Int,
    val completedDhikr: Int,
) {
    val isComplete: Boolean get() = totalDhikr > 0 && completedDhikr >= totalDhikr
    val fraction: Float get() = if (totalDhikr <= 0) 0f else completedDhikr.toFloat() / totalDhikr
}
