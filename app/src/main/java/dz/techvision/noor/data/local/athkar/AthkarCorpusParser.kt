package dz.techvision.noor.data.local.athkar

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import dz.techvision.noor.domain.model.athkar.Dhikr
import dz.techvision.noor.domain.model.athkar.DhikrCategory
import dz.techvision.noor.domain.model.athkar.DhikrCount
import dz.techvision.noor.domain.repository.AthkarProvenance

/**
 * Parses the unmodified `rn0x/Adhkar-json` asset.
 *
 * The source is a JSON array. Each category has `id`, `category`, `audio`,
 * `filename`, and `array`; each entry in `array` has `id`, `text`, `count`,
 * `audio`, and `filename`. Audio paths are retained only in the source asset:
 * the feature is text-only until matching audio files can also be bundled.
 */
class AthkarCorpusParser(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): AthkarCorpus {
        val source = try {
            gson.fromJson<List<SourceCategory>>(json, CATEGORY_LIST_TYPE)
        } catch (error: JsonParseException) {
            throw AthkarCorpusFormatException("adhkar asset is not a JSON array", error)
        } ?: throw AthkarCorpusFormatException("adhkar asset is empty")
        if (source.isEmpty()) throw AthkarCorpusFormatException("adhkar asset is empty")

        val categories = source.mapIndexed { index, category ->
            category.requireValid(index)
            DhikrCategory(
                id = categoryKey(category.id),
                order = index,
                titleResKey = category.category,
            )
        }
        val dhikr = source.flatMapIndexed { categoryIndex, category ->
            category.array.mapIndexed { entryIndex, entry ->
                entry.requireValid(categoryIndex, entryIndex)
                Dhikr(
                    id = "${categoryKey(category.id)}-${entry.id}",
                    categoryId = categoryKey(category.id),
                    arabicText = entry.text,
                    repeatCount = DhikrCount.Prescribed(entry.count),
                    order = entryIndex,
                )
            }
        }

        return AthkarCorpus(
            schemaVersion = AthkarCorpus.SCHEMA_VERSION,
            provenance = AthkarProvenance(
                corpusId = "rn0x/Adhkar-json",
                title = "Adhkar-json",
                author = "rn0x",
                license = "No formal license; the repository README states the files may be used freely.",
                sourceUrl = SOURCE_URL,
                isContentBundled = true,
                bundledDhikrCount = dhikr.size,
                note = "Text from Hisn al-Muslim as identified by the source repository.",
            ),
            categories = categories,
            dhikr = dhikr,
        )
    }

    private fun SourceCategory.requireValid(index: Int) {
        if (id <= 0) throw AthkarCorpusFormatException("categories[$index].id must be positive")
        if (category.isBlank()) throw AthkarCorpusFormatException("categories[$index].category is blank")
        if (array.isEmpty()) throw AthkarCorpusFormatException("categories[$index].array is empty")
    }

    private fun SourceDhikr.requireValid(categoryIndex: Int, entryIndex: Int) {
        val label = "categories[$categoryIndex].array[$entryIndex]"
        if (id <= 0) throw AthkarCorpusFormatException("$label.id must be positive")
        if (text.isBlank()) throw AthkarCorpusFormatException("$label.text is blank")
        if (count <= 0) throw AthkarCorpusFormatException("$label.count must be positive")
    }

    private fun categoryKey(sourceId: Int): String = "source-$sourceId"

    private data class SourceCategory(
        val id: Int = 0,
        val category: String = "",
        val audio: String? = null,
        val filename: String? = null,
        val array: List<SourceDhikr> = emptyList(),
    )

    private data class SourceDhikr(
        val id: Int = 0,
        val text: String = "",
        val count: Int = 0,
        val audio: String? = null,
        val filename: String? = null,
    )

    private companion object {
        val CATEGORY_LIST_TYPE = object : TypeToken<List<SourceCategory>>() {}.type
        const val SOURCE_URL = "https://github.com/rn0x/Adhkar-json"
    }
}

/** The asset is not the documented source shape. */
class AthkarCorpusFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

