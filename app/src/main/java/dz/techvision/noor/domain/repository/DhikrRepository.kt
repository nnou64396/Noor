package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.athkar.Dhikr
import dz.techvision.noor.domain.model.athkar.DhikrCategory

/**
 * Read-only access to the bundled, offline adhkar corpus.
 *
 * The corpus ships inside the APK, so every call is satisfied without a network
 * connection. Implementations return the same immutable data for the life of the
 * process; the UI observes them through the ViewModel rather than re-reading the
 * asset per frame.
 */
interface DhikrRepository {

    /**
     * Categories declared by the corpus, ordered as the source orders them.
     * Empty when no corpus has been bundled yet.
     */
    suspend fun categories(): List<DhikrCategory>

    /** Adhkar of one category, ordered by the source. */
    suspend fun dhikrIn(categoryId: String): List<Dhikr>

    /** Every adhkar in the corpus, in source order across categories. */
    suspend fun allDhikr(): List<Dhikr>

    /**
     * Human-readable description of what is currently bundled and where it came
     * from, for the "about this corpus" surface. Never null so the UI can always
     * explain the source of what it shows.
     */
    suspend fun provenanceSummary(): AthkarProvenance
}

/** Where the bundled corpus came from and what may be redistributed. */
data class AthkarProvenance(
    val corpusId: String,
    val title: String,
    val author: String? = null,
    val edition: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val sourceUrl: String? = null,
    /** False when the app ships the pipeline but no content has been cleared. */
    val isContentBundled: Boolean,
    val bundledDhikrCount: Int,
    /** Why no content is bundled, when [isContentBundled] is false. */
    val note: String? = null,
)

