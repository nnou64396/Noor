package com.noor.islamic.data.local.athkar

import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.athkar.DhikrCount
import com.noor.islamic.domain.model.athkar.DhikrProgress
import com.noor.islamic.domain.repository.AthkarProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * The bundled-corpus pipeline on the JVM: the real `org.json` implementation and
 * the real APK asset, so parsing, validation and the shipped file are all checked
 * without a device.
 */
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

        assertEquals(AthkarCorpus.SCHEMA_VERSION, corpus.schemaVersion)
        assertEquals(emptyList<String>(), validator.validate(corpus))
    }

    @Test
    fun shippedAsset_bundlesNoContentAndSaysWhy() = runBlocking {
        val corpus = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
        ).load()

        // Nothing is fabricated while no cleared source exists: no adhkar, no
        // categories, and the provenance is explicit about it.
        assertFalse(corpus.isContentBundled)
        assertEquals(0, corpus.dhikr.size)
        assertEquals(0, corpus.categories.size)
        assertEquals(0, corpus.expectedDhikrCount)
        assertFalse(corpus.provenance.isContentBundled)
        assertTrue(corpus.provenance.corpusId.isNotBlank())
        assertTrue(corpus.provenance.note?.isNotBlank() == true)
    }

    @Test
    fun parser_readsCategoriesDhikrAndProvenance() {
        val corpus = parser.parse(corpusJson())

        assertEquals(1, corpus.categories.size)
        assertEquals("morning", corpus.categories.single().id)
        assertEquals("Morning adhkar", corpus.provenance.title)
        assertEquals(1, corpus.provenance.bundledDhikrCount)
        assertTrue(corpus.isContentBundled)

        val dhikr = corpus.dhikr.single()
        assertEquals("morning.subhanallah", dhikr.id)
        assertEquals("morning", dhikr.categoryId)
        assertEquals(DhikrCount.Prescribed(33), dhikr.repeatCount)
        assertEquals(0, dhikr.order)
        assertEquals("Reference", dhikr.reference)
        assertEquals("Translation", dhikr.translation)
    }

    @Test
    fun parser_keepsOptionalTextNullRatherThanEmpty() {
        val corpus = parser.parse(corpusJson(includeTranslation = false, includeReference = false))

        val dhikr = corpus.dhikr.single()
        assertNull(dhikr.translation)
        assertNull(dhikr.reference)
    }

    @Test
    fun parser_rejectsAnUnsupportedSchemaVersion() {
        val json = corpusJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")

        val error = runCatching { parser.parse(json) }.exceptionOrNull()

        assertTrue(error is AthkarCorpusFormatException)
        assertTrue(error!!.message!!.contains("schemaVersion"))
    }

    @Test
    fun parser_rejectsAMissingRequiredField() {
        val json = corpusJson().replace("\"arabicText\": \"سُبْحَانَ اللَّهِ\",", "")

        val error = runCatching { parser.parse(json) }.exceptionOrNull()

        assertTrue(error is AthkarCorpusFormatException)
        assertTrue(error!!.message!!.contains("arabicText"))
    }

    @Test
    fun parser_rejectsANonObjectDocument() {
        val error = runCatching { parser.parse("[]") }.exceptionOrNull()

        assertTrue(error is AthkarCorpusFormatException)
    }

    @Test
    fun validator_rejectsAStableIdCollision() {
        val json = corpusJsonWithTwoDhikrSharingAnId()

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.any { it.contains("duplicate dhikr id") })
    }

    @Test
    fun validator_acceptsAWellFormedCorpus() {
        assertEquals(emptyList<String>(), validator.validate(parser.parse(corpusJson())))
    }

    @Test
    fun validator_rejectsNonArabicText() {
        val json = corpusJson().replace("سُبْحَانَ اللَّهِ", "Subhan Allah")

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.isNotEmpty())
        assertTrue(problems.any { it.contains("Arabic") })
    }

    @Test
    fun validator_rejectsAMissingReference() {
        val json = corpusJson(includeReference = false)

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.any { it.contains("reference") })
    }

    @Test
    fun validator_rejectsANonPositivePrescribedRepetitionCount() {
        listOf("0", "-1", "-33").forEach { token ->
            val problems = validator.validate(parser.parse(corpusJsonWithRepeatCountToken(token)))

            assertTrue(
                "expected $token to be rejected, got $problems",
                problems.any { it.contains("repeatCount") },
            )
        }
    }

    @Test
    fun validator_acceptsAnyPositivePrescribedRepetitionCount() {
        listOf("1", "3", "7", "33", "100").forEach { token ->
            val problems = validator.validate(parser.parse(corpusJsonWithRepeatCountToken(token)))

            assertEquals("expected $token to be accepted, got $problems", emptyList<String>(), problems)
        }
    }

    @Test
    fun validator_acceptsAnUnspecifiedRepetitionCount() {
        val problems = validator.validate(parser.parse(corpusJsonWithRepeatCountToken("null")))

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun parser_readsAPositiveIntegerAsAPrescribedCount() {
        listOf("1", "3", "7", "33", "100").forEach { token ->
            val dhikr = parser.parse(corpusJsonWithRepeatCountToken(token)).dhikr.single()

            assertEquals(DhikrCount.Prescribed(token.toInt()), dhikr.repeatCount)
        }
    }

    @Test
    fun parser_readsAnExplicitNullAsUnspecified() {
        val dhikr = parser.parse(corpusJsonWithRepeatCountToken("null")).dhikr.single()

        assertEquals(DhikrCount.Unspecified, dhikr.repeatCount)
    }

    @Test
    fun parser_keepsAnUnspecifiedCountDistinctFromEveryStruckCount() {
        val dhikr = parser.parse(corpusJsonWithRepeatCountToken("null")).dhikr.single()

        // The whole point of the type: an absent count must never come back as a
        // number, least of all one of the counts tradition commonly uses.
        listOf(1, 3, 7, 33, 100).forEach { invented ->
            assertFalse(
                "Unspecified must not become $invented",
                dhikr.repeatCount == DhikrCount.Prescribed(invented),
            )
        }
        assertNull(DhikrProgress.targetOf(dhikr.repeatCount))
    }

    @Test
    fun parser_rejectsAMissingRepetitionCount() {
        // Renamed away, so `repeatCount` is genuinely absent rather than null.
        val json = corpusJson().replace("\"repeatCount\": 33", "\"renamedCount\": 33")

        val error = runCatching { parser.parse(json) }.exceptionOrNull()

        assertTrue(error is AthkarCorpusFormatException)
        assertTrue(error!!.message!!.contains("repeatCount"))
    }

    @Test
    fun parser_rejectsANonIntegerRepeatCount() {
        // org.json's optInt would silently coerce every one of these into a real
        // prescribed number, which is exactly the fabrication this guards against.
        listOf("\"33\"", "33.5", "true", "{}", "[]", "\"\"", "\"thirty-three\"").forEach { token ->
            val error = runCatching {
                parser.parse(corpusJsonWithRepeatCountToken(token))
            }.exceptionOrNull()

            assertTrue(
                "expected $token to be a format error, got $error",
                error is AthkarCorpusFormatException,
            )
        }
    }

    @Test
    fun parser_carriesANonPositiveIntegerThroughForTheValidatorToReject() {
        val dhikr = parser.parse(corpusJsonWithRepeatCountToken("0")).dhikr.single()

        // A non-positive count is semantically invalid rather than malformed, so it
        // survives the parse-then-validate split and is rejected with a precise reason.
        assertEquals(DhikrCount.Prescribed(0), dhikr.repeatCount)
        assertTrue(validator.validate(parser.parse(corpusJsonWithRepeatCountToken("0"))).isNotEmpty())
    }

    @Test
    fun validator_rejectsADhikrWithoutACategory() {
        val json = corpusJson().replace("\"categoryId\": \"morning\"", "\"categoryId\": \"nope\"")

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.isNotEmpty())
    }

    @Test
    fun validator_rejectsACountThatDisagreesWithThePayload() {
        val json = corpusJson().replace("\"bundledDhikrCount\": 1", "\"bundledDhikrCount\": 7")

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.any { it.contains("bundledDhikrCount") || it.contains("declared") })
    }

    @Test
    fun validator_rejectsContentWithoutALicense() {
        val json = corpusJson().replace("\"license\": \"CC0-1.0\",", "\"license\": \"\",")
            .replace("\"licenseUrl\": \"https://creativecommons.org/publicdomain/zero/1.0/\",", "")

        val problems = validator.validate(parser.parse(json))

        assertTrue(problems.any { it.contains("license") })
    }

    @Test
    fun validator_doesNotRejectCategoriesTheAppCannotLocalize() {
        val json = corpusJson().replace("\"id\": \"morning\"", "\"id\": \"after_rain\"")
            .replace("\"categoryId\": \"morning\"", "\"categoryId\": \"after_rain\"")

        val corpus = parser.parse(json)

        assertEquals(emptyList<String>(), validator.validate(corpus))
        assertEquals(listOf("after_rain"), validator.unlocalizedCategoryIds(corpus))
    }

    @Test
    fun corpus_filtersDhikrByCategory() {
        val corpus = AthkarCorpus(
            schemaVersion = 1,
            provenance = AthkarProvenance(
                corpusId = "c",
                title = "t",
                isContentBundled = true,
                bundledDhikrCount = 2,
            ),
            categories = listOf(
                DhikrCategory("morning", 0, "Morning adhkar"),
                DhikrCategory("evening", 1, "Evening adhkar"),
            ),
            dhikr = listOf(
                dhikr("morning.1", "morning", 0),
                dhikr("evening.1", "evening", 0),
            ),
        )

        assertEquals(listOf("morning.1"), corpus.dhikrIn("morning").map { it.id })
        assertEquals(listOf("evening.1"), corpus.dhikrIn("evening").map { it.id })
        assertEquals(emptyList<Dhikr>(), corpus.dhikrIn("unknown"))
    }

    @Test
    fun source_reportsAnEmptyCorpusWhenTheAssetIsMissing() = runBlocking {
        val corpus = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            assetPath = "athkar/does-not-exist.json",
            ioDispatcher = Dispatchers.Unconfined,
        ).load()

        assertFalse(corpus.isContentBundled)
        assertEquals(0, corpus.dhikr.size)
        assertTrue(corpus.provenance.note?.contains("could not be read") == true)
    }

    @Test
    fun source_dropsARejectedCorpusInsteadOfPartiallyShowingIt() = runBlocking {
        val rejected = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
            assetReader = { rejectedCorpusJson() },
        ).load()

        assertEquals(0, rejected.dhikr.size)
        assertEquals(0, rejected.categories.size)
        assertFalse(rejected.isContentBundled)
        assertTrue(rejected.provenance.note?.contains("rejected") == true)
    }

    @Test
    fun source_reportsAnEmptyCorpusWhenTheDocumentIsMalformed() = runBlocking {
        val malformed = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
            assetReader = { "{" },
        ).load()

        assertFalse(malformed.isContentBundled)
        assertEquals(0, malformed.dhikr.size)
        assertTrue(malformed.provenance.note?.contains("malformed") == true)
    }

    @Test
    fun source_cachesTheCorpusAfterTheFirstRead() = runBlocking {
        val source = AssetAthkarCorpusSource(
            context = RuntimeEnvironment.getApplication(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(source.load(), source.load())
    }

    private fun dhikr(id: String, categoryId: String, order: Int) = Dhikr(
        id = id,
        categoryId = categoryId,
        arabicText = "سُبْحَانَ اللَّهِ",
        translation = null,
        repeatCount = DhikrCount.Prescribed(3),
        reference = "Reference",
        order = order,
    )

    private fun corpusJson(
        includeTranslation: Boolean = true,
        includeReference: Boolean = true,
    ): String = """
        {
          "schemaVersion": 1,
          "provenance": {
            "corpusId": "test.corpus",
            "title": "Morning adhkar",
            "author": "Author",
            "edition": "Edition",
            "license": "CC0-1.0",
            "licenseUrl": "https://creativecommons.org/publicdomain/zero/1.0/",
            "sourceUrl": "https://example.org",
            "isContentBundled": true,
            "bundledDhikrCount": 1,
            "note": ""
          },
          "categories": [
            { "id": "morning", "order": 0, "titleResKey": "Morning adhkar" }
          ],
          "adhkar": [
            {
              "id": "morning.subhanallah",
              "categoryId": "morning",
              "arabicText": "سُبْحَانَ اللَّهِ",
              ${if (includeTranslation) "\"translation\": \"Translation\"," else ""}
              "repeatCount": 33,
              ${if (includeReference) "\"reference\": \"Reference\"," else ""}
              "order": 0
            }
          ]
        }
    """.trimIndent()

    /** Rewrites the declared count with a raw JSON token, so type errors are exercised. */
    private fun corpusJsonWithRepeatCountToken(token: String): String =
        corpusJson().replace("\"repeatCount\": 33", "\"repeatCount\": $token")

    /** Two dhikr in one category that share a stable id, which must be rejected. */
    private fun corpusJsonWithTwoDhikrSharingAnId(): String = """
        {
          "schemaVersion": 1,
          "provenance": {
            "corpusId": "test.corpus",
            "title": "Morning adhkar",
            "license": "CC0-1.0",
            "licenseUrl": "https://creativecommons.org/publicdomain/zero/1.0/",
            "isContentBundled": true,
            "bundledDhikrCount": 2
          },
          "categories": [
            { "id": "morning", "order": 0, "titleResKey": "Morning adhkar" }
          ],
          "adhkar": [
            {
              "id": "morning.duplicate",
              "categoryId": "morning",
              "arabicText": "سُبْحَانَ اللَّهِ",
              "repeatCount": 3,
              "reference": "Reference",
              "order": 0
            },
            {
              "id": "morning.duplicate",
              "categoryId": "morning",
              "arabicText": "الْحَمْدُ لِلَّهِ",
              "repeatCount": 3,
              "reference": "Reference",
              "order": 1
            }
          ]
        }
    """.trimIndent()

    companion object {
        /** A fixture that parses as JSON but breaks the content rules on purpose. */
        private fun rejectedCorpusJson(): String = """
            {
              "schemaVersion": 1,
              "provenance": {
                "corpusId": "test.rejected",
                "title": "Deliberately invalid fixture",
                "license": "",
                "isContentBundled": true,
                "bundledDhikrCount": 1
              },
              "categories": [
                { "id": "morning", "order": 0, "titleResKey": "Morning adhkar" }
              ],
              "adhkar": [
                {
                  "id": "morning.bad",
                  "categoryId": "morning",
                  "arabicText": "not arabic at all",
                  "repeatCount": 3,
                  "reference": "",
                  "order": 0
                }
              ]
            }
        """.trimIndent()
    }
}
