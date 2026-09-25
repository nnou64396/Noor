package com.noor.islamic.domain.model.athkar

/**
 * How many repetitions a dhikr requires, as an explicit two-state decision made
 * by the corpus.
 *
 * A nullable count was rejected because `repeatCount ?: 1` and
 * `target.coerceAtLeast(1)` both turn "no count is prescribed" into an invented
 * religious requirement, silently and invisibly. With a sealed type there is no
 * number to fall back on: every caller must state which of the two cases it is
 * handling, and omitting the `Unspecified` case is a compile error.
 *
 * [Prescribed] deliberately accepts any integer so that a corpus declaring `0` or
 * a negative count can be *rejected with a precise reason* by
 * `AthkarCorpusValidator` rather than crashing the parser — the same
 * parse-then-validate shape already used for `order`. A corpus containing one is
 * dropped whole, so no such value ever reaches a ViewModel or the screen.
 */
sealed interface DhikrCount {

    /** A count the source states explicitly. Valid values are 1 and above. */
    data class Prescribed(val value: Int) : DhikrCount

    /** The source states no count, so the app must never display or infer one. */
    data object Unspecified : DhikrCount
}

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
    val repeatCount: DhikrCount,
    val reference: String? = null,
    val order: Int,
)

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

/**
 * How much of a category the user has completed so far.
 *
 * [totalDhikr] counts every dhikr in the category, while [prescribedDhikr]
 * counts only those the corpus gives a repetition count to. Completion is
 * measured against [prescribedDhikr] so that a dhikr with no prescribed count
 * neither inflates the denominator nor produces a percentage built on a target
 * that does not exist.
 */
data class CategoryProgress(
    val categoryId: String,
    val totalDhikr: Int,
    val completedDhikr: Int,
    val prescribedDhikr: Int = 0,
) {
    val hasPrescribedCounts: Boolean get() = prescribedDhikr > 0
    val isComplete: Boolean get() = hasPrescribedCounts && completedDhikr >= prescribedDhikr
    val fraction: Float
        get() = if (prescribedDhikr <= 0) 0f else completedDhikr.toFloat() / prescribedDhikr
}
