package com.noor.islamic.data.local.athkar

import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.repository.AthkarProvenance
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the bundled adhkar asset (`assets/athkar/adhkar.json`).
 *
 * Parsing is intentionally strict about structure (types, required fields) and
 * silent about content quality: quality rules live in [AthkarCorpusValidator] so
 * that a malformed file and an unacceptable file produce different, testable
 * failures. No text is ever rewritten, normalised or generated here.
 */
class AthkarCorpusParser(
    private val supportedSchemaVersion: Int = AthkarCorpus.SCHEMA_VERSION,
) {

    /**
     * @throws AthkarCorpusFormatException when the document is not the expected
     *   shape. Callers treat this as "no corpus" rather than a crash.
     */
    fun parse(json: String): AthkarCorpus {
        val root = try {
            JSONObject(json)
        } catch (error: Exception) {
            throw AthkarCorpusFormatException("adhkar asset is not a JSON object", error)
        }

        val schemaVersion = root.optIntOrThrow("schemaVersion")
        if (schemaVersion != supportedSchemaVersion) {
            throw AthkarCorpusFormatException(
                "unsupported schemaVersion $schemaVersion (expected $supportedSchemaVersion)",
            )
        }

        val provenance = root.optJSONObjectOrThrow("provenance").toProvenance()
        val categories = root.optJSONArrayOrThrow("categories").mapObjects { it.toCategory() }
        val dhikr = root.optJSONArrayOrThrow("adhkar").mapObjects { it.toDhikr() }

        return AthkarCorpus(
            schemaVersion = schemaVersion,
            provenance = provenance,
            categories = categories,
            dhikr = dhikr,
        )
    }

    private fun JSONObject.toProvenance() = AthkarProvenance(
        corpusId = optStringOrThrow("corpusId"),
        title = optStringOrThrow("title"),
        author = optStringOrEmpty("author"),
        edition = optStringOrEmpty("edition"),
        license = optStringOrEmpty("license"),
        licenseUrl = optStringOrEmpty("licenseUrl"),
        sourceUrl = optStringOrEmpty("sourceUrl"),
        isContentBundled = optBoolean("isContentBundled", false),
        bundledDhikrCount = optInt("bundledDhikrCount", 0),
        note = optStringOrEmpty("note"),
    )

    private fun JSONObject.toCategory() = DhikrCategory(
        id = optStringOrThrow("id"),
        order = optInt("order", 0),
        titleResKey = optStringOrEmpty("titleResKey").ifEmpty { optStringOrThrow("id") },
    )

    private fun JSONObject.toDhikr() = Dhikr(
        id = optStringOrThrow("id"),
        categoryId = optStringOrThrow("categoryId"),
        arabicText = optStringOrThrow("arabicText"),
        translation = optStringOrEmpty("translation").ifEmpty { null },
        repeatCount = optInt("repeatCount", 0),
        reference = optStringOrEmpty("reference").ifEmpty { null },
        order = optInt("order", 0),
    )

    private fun JSONObject.optIntOrThrow(key: String): Int =
        if (has(key)) optInt(key) else throw AthkarCorpusFormatException("missing $key")

    private fun JSONObject.optStringOrThrow(key: String): String {
        if (!has(key)) throw AthkarCorpusFormatException("missing $key")
        val value = optString(key)
        if (value.isEmpty()) throw AthkarCorpusFormatException("blank $key")
        return value
    }

    private fun JSONObject.optStringOrEmpty(key: String): String =
        if (has(key) && !isNull(key)) optString(key) else ""

    private fun JSONObject.optJSONObjectOrThrow(key: String): JSONObject =
        optJSONObject(key) ?: throw AthkarCorpusFormatException("missing object $key")

    private fun JSONObject.optJSONArrayOrThrow(key: String): JSONArray =
        optJSONArray(key) ?: throw AthkarCorpusFormatException("missing array $key")

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { index ->
            transform(optJSONObject(index) ?: throw AthkarCorpusFormatException("item $index is not an object"))
        }
}

/** The asset is not the documented shape. Distinct from a validator rejection. */
class AthkarCorpusFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
