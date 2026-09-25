package com.noor.islamic.data.local.athkar

import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.repository.AthkarProvenance

/**
 * A parsed, read-only adhkar corpus as it exists in the bundle.
 *
 * [isContentBundled] is false while the app ships only the loading pipeline; in
 * that case [dhikr] is empty and [provenance].note explains what is still
 * missing. Nothing here is ever fabricated to fill the UI: with no cleared
 * source there are no categories and no adhkar.
 */
data class AthkarCorpus(
    val schemaVersion: Int,
    val provenance: AthkarProvenance,
    val categories: List<DhikrCategory> = emptyList(),
    val dhikr: List<Dhikr> = emptyList(),
) {
    val isContentBundled: Boolean get() = provenance.isContentBundled && dhikr.isNotEmpty()

    fun dhikrIn(categoryId: String): List<Dhikr> = dhikr.filter { it.categoryId == categoryId }

    /** The pinned, source-declared dhikr total, or the actual size when unset. */
    val expectedDhikrCount: Int
        get() = provenance.bundledDhikrCount

    companion object {
        /**
         * The pipeline with nothing bundled. Used when the asset is missing or
         * unparseable so the feature degrades to an explained empty state rather
         * than throwing on startup.
         */
        fun empty(note: String? = null) = AthkarCorpus(
            schemaVersion = SCHEMA_VERSION,
            provenance = AthkarProvenance(
                corpusId = NONE_CORPUS_ID,
                title = NONE_CORPUS_TITLE,
                isContentBundled = false,
                bundledDhikrCount = 0,
                note = note,
            ),
        )

        const val SCHEMA_VERSION = 1
        const val NONE_CORPUS_ID = "none"
        const val NONE_CORPUS_TITLE = "No adhkar corpus bundled"
    }
}

/**
 * Supplies the read-only corpus.
 *
 * The one production implementation reads the bundled asset, but the repository
 * depends on this interface so content and progress can be tested separately.
 */
interface AthkarCorpusLoader {
    suspend fun load(): AthkarCorpus
}
