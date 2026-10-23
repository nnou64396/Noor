package com.noor.islamic.data.local.athkar

import com.noor.islamic.domain.model.athkar.DhikrCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks the real rn0x source shape and the bundled offline asset. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AthkarCorpusTest {

    private val parser = AthkarCorpusParser()
    private val validator = AthkarCorpusValidator()

    @Test
    fun shippedAsset_parsesAndValidates() = runBlocking {
        val corpus = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
        ).load()

        assertTrue(corpus.isContentBundled)
        assertEquals(132, corpus.categories.size)
        assertEquals(267, corpus.dhikr.size)
        assertEquals(corpus.dhikr.size, corpus.expectedDhikrCount)
        assertEquals(emptyList<String>(), validator.validate(corpus))

        // The shipped asset must actually contain usable Arabic content, not
        // merely the right number of entries.
        assertTrue(corpus.categories.all { it.titleResKey.isNotBlank() })
        assertTrue(corpus.categories.all { category -> corpus.dhikrIn(category.id).isNotEmpty() })
        assertTrue(
            corpus.dhikr.all { dhikr ->
                dhikr.arabicText.any { character -> character in '\u0600'..'\u06FF' }
            },
        )
        assertTrue(
            corpus.dhikr.all { dhikr ->
                val count = dhikr.repeatCount
                count is DhikrCount.Prescribed && count.value > 0
            },
        )
    }

    @Test
    fun validator_rejectsDeclaredCountThatDoesNotMatchTheCorpus() {
        val corpus = parser.parse(sourceJson())
        val tampered = corpus.copy(
            provenance = corpus.provenance.copy(bundledDhikrCount = corpus.dhikr.size + 1),
        )

        val problems = validator.validate(tampered)

        assertTrue(problems.any { "declared bundledDhikrCount" in it })
    }

    @Test
    fun validator_rejectsInvalidContentEvenWhenTheJsonParses() {
        val corpus = parser.parse(sourceJson())
        val tampered = corpus.copy(
            dhikr = corpus.dhikr + corpus.dhikr.single().copy(order = 1),
        )

        val problems = validator.validate(tampered)

        assertTrue(problems.any { "duplicate dhikr id" in it })
    }

    @Test
    fun parser_usesSourceCategoryTextAndCountWithoutChangingArabicText() {
        val corpus = parser.parse(sourceJson())

        assertEquals("أذكار الصباح والمساء", corpus.categories.single().titleResKey)
        val dhikr = corpus.dhikr.single()
        assertEquals("سُبْحَانَ اللَّهِ", dhikr.arabicText)
        assertEquals(DhikrCount.Prescribed(33), dhikr.repeatCount)
        assertEquals("source-1-1", dhikr.id)
        assertEquals("source-1", dhikr.categoryId)
        assertTrue(corpus.provenance.isContentBundled)
        assertEquals("rn0x/Adhkar-json", corpus.provenance.corpusId)
    }

    @Test
    fun parser_rejectsMalformedOrIncompleteSourceData() {
        assertTrue(runCatching { parser.parse("{") }.exceptionOrNull() is AthkarCorpusFormatException)
        assertTrue(
            runCatching {
                parser.parse(sourceJson().replace("\"count\": 33", "\"count\": 0"))
            }.exceptionOrNull() is AthkarCorpusFormatException,
        )
    }

    @Test
    fun source_reportsEmptyCorpusWhenAssetCannotBeRead() = runBlocking {
        val corpus = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            assetPath = "athkar/missing.json",
            ioDispatcher = Dispatchers.Unconfined,
        ).load()

        assertFalse(corpus.isContentBundled)
        assertTrue(corpus.provenance.note?.contains("could not be read") == true)
    }

    @Test
    fun source_reportsEmptyCorpusWhenAssetIsMalformed() = runBlocking {
        val corpus = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            assetReader = { "{" },
            ioDispatcher = Dispatchers.Unconfined,
        ).load()

        assertFalse(corpus.isContentBundled)
        assertTrue(corpus.provenance.note?.contains("malformed") == true)
    }

    private fun sourceJson() = """
        [
          {
            "id": 1,
            "category": "أذكار الصباح والمساء",
            "audio": "/audio/category.mp3",
            "filename": "category",
            "array": [
              {
                "id": 1,
                "text": "سُبْحَانَ اللَّهِ",
                "count": 33,
                "audio": "/audio/one.mp3",
                "filename": "one"
              }
            ]
          }
        ]
    """.trimIndent()
}
