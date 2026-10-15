package com.noor.islamic.data.local.athkar

import com.noor.islamic.domain.model.athkar.DhikrCount

/**
 * Deterministic acceptance rules for a bundled adhkar corpus.
 *
 * Religious text is never accepted on trust, so the rules are mechanical and
 * exhaustive: every rule below is checked for every dhikr, and a single failure
 * rejects the whole corpus. The validator never repairs, rewrites or completes
 * anything — it can only say "yes" or "here is what is wrong", because quietly
 * fixing religious text is exactly the failure mode this project must avoid.
 *
 * The rules exist to catch the things a human reviewer cannot eyeball reliably
 * across a large file: duplicate ids, blank text, dhikr pointing at a category
 * that does not exist, a prescribed repetition count that is not a real count,
 * colliding ordering, a declared total that disagrees with the payload, and
 * missing attribution.
 */
class AthkarCorpusValidator(
    /**
     * Category ids the app can render a title for. A corpus may use other ids
     * (the UI falls back to a generic label), so these are informational rather
     * than a hard requirement.
     */
    private val knownCategoryIds: Set<String> = KNOWN_CATEGORY_IDS,
) {

    /** Returns every problem found; an empty list means the corpus is acceptable. */
    fun validate(corpus: AthkarCorpus): List<String> {
        val problems = mutableListOf<String>()

        validateProvenance(corpus, problems)
        validateCategories(corpus, problems)
        validateDhikr(corpus, problems)

        return problems
    }

    /** Throws when unacceptable; used at the single load site. */
    fun requireValid(corpus: AthkarCorpus): AthkarCorpus {
        val problems = validate(corpus)
        if (problems.isNotEmpty()) {
            throw AthkarCorpusValidationException(problems)
        }
        return corpus
    }

    private fun validateProvenance(corpus: AthkarCorpus, problems: MutableList<String>) {
        if (corpus.provenance.isContentBundled) {
            if (corpus.provenance.license.isNullOrBlank()) {
                problems += "provenance: license is required when content is bundled"
            }
            if (corpus.provenance.bundledDhikrCount != corpus.dhikr.size) {
                problems += "provenance: declared bundledDhikrCount " +
                    "${corpus.provenance.bundledDhikrCount} but the corpus has ${corpus.dhikr.size} adhkar"
            }
            if (corpus.dhikr.isEmpty()) {
                problems += "provenance: isContentBundled is true but no adhkar are present"
            }
        } else {
            if (corpus.dhikr.isNotEmpty()) {
                problems += "provenance: content is present but isContentBundled is false"
            }
        }
    }

    private fun validateCategories(corpus: AthkarCorpus, problems: MutableList<String>) {
        val seenIds = mutableSetOf<String>()
        val seenOrders = mutableSetOf<Int>()

        corpus.categories.forEachIndexed { index, category ->
            val label = "categories[$index] (${category.id})"
            if (category.id.isBlank()) problems += "$label: id must not be blank"
            if (!seenIds.add(category.id)) problems += "$label: duplicate category id '${category.id}'"
            if (category.order < 0) problems += "$label: order must be >= 0"
            if (!seenOrders.add(category.order)) {
                problems += "$label: duplicate category order ${category.order}"
            }
        }
    }

    private fun validateDhikr(corpus: AthkarCorpus, problems: MutableList<String>) {
        val categoryIds = corpus.categories.mapTo(mutableSetOf()) { it.id }
        val seenIds = mutableSetOf<String>()
        val orderByCategory = mutableMapOf<String, MutableSet<Int>>()

        corpus.dhikr.forEachIndexed { index, dhikr ->
            val label = "adhkar[$index] (${dhikr.id})"

            if (dhikr.id.isBlank()) {
                problems += "$label: id must not be blank"
            } else if (!seenIds.add(dhikr.id)) {
                problems += "$label: duplicate dhikr id '${dhikr.id}'"
            }

            if (dhikr.arabicText.isBlank()) {
                problems += "$label: arabicText must not be empty"
            } else if (dhikr.arabicText.none { it.isArabicLetter() }) {
                problems += "$label: arabicText must contain Arabic script"
            }

            if (dhikr.categoryId.isBlank()) {
                problems += "$label: categoryId must not be blank"
            } else if (categoryIds.isNotEmpty() && dhikr.categoryId !in categoryIds) {
                problems += "$label: categoryId '${dhikr.categoryId}' is not declared in categories"
            }

            when (val count = dhikr.repeatCount) {
                is DhikrCount.Unspecified -> Unit

                is DhikrCount.Prescribed ->
                    if (count.value <= 0) {
                        problems += "$label: repeatCount must be > 0 when a count is " +
                            "prescribed (was ${count.value}); use null for no prescribed count"
                    }
            }

            if (dhikr.order < 0) {
                problems += "$label: order must be >= 0"
            } else {
                val orders = orderByCategory.getOrPut(dhikr.categoryId) { mutableSetOf() }
                if (!orders.add(dhikr.order)) {
                    problems += "$label: duplicate order ${dhikr.order} within category '${dhikr.categoryId}'"
                }
            }
        }
    }

    /**
     * Category ids the corpus uses for which the app has no localized title. These
     * are never a rejection reason: a corpus may add categories the app does not
     * translate yet, and the UI falls back to a generic title.
     */
    fun unlocalizedCategoryIds(corpus: AthkarCorpus): List<String> =
        corpus.categories.map { it.id }.filterNot { it in knownCategoryIds }.sorted()

    companion object {
        /** Slugs the app ships localized titles for. */
        val KNOWN_CATEGORY_IDS: Set<String> = setOf(
            "morning",
            "evening",
            "after_prayer",
            "before_sleep",
            "general",
        )
    }
}

/** Arabic letters only, so diacritics and punctuation are ignored. */
private fun Char.isArabicLetter(): Boolean = this in '\u0600'..'\u06FF'

/** The corpus parsed correctly but failed the content rules. */
class AthkarCorpusValidationException(val problems: List<String>) :
    Exception("adhkar corpus rejected: " + problems.joinToString("; "))
