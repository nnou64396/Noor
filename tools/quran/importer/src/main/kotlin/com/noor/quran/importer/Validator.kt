package com.noor.quran.importer

import com.noor.quran.common.ArabicSearchNormalizer
import com.noor.quran.common.AyahRef
import com.noor.quran.common.QuranMetadata
import com.noor.quran.common.QuranText
import com.noor.quran.common.SurahText
import kotlin.math.max

/**
 * Validates the Tanzil inputs plus derived structures before any database write.
 * Throws IllegalArgumentException with a full report when anything is off.
 */
object Validator {

    const val EXPECTED_SURAH_COUNT = 114
    const val EXPECTED_AYAH_COUNT = 6236
    const val EXPECTED_JUZ_COUNT = 30
    const val EXPECTED_PAGE_COUNT = 604
    const val EXPECTED_SAJDA_COUNT = 15

    fun validate(
        xmlText: QuranText,
        txtLines: List<String>,
        txt2: List<com.noor.quran.common.QuranAyah>,
        metadata: QuranMetadata,
    ) {
        val failures = mutableListOf<String>()

        // --- XML textual stream -------------------------------------------------
        if (xmlText.surahs.size != EXPECTED_SURAH_COUNT) {
            failures += "xml surah count ${xmlText.surahs.size} != $EXPECTED_SURAH_COUNT"
        }
        if (xmlText.totalAyahs != EXPECTED_AYAH_COUNT) {
            failures += "xml ayah count ${xmlText.totalAyahs} != $EXPECTED_AYAH_COUNT"
        }
        val surahNumbers = xmlText.surahs.map { it.surahNumber }
        if (surahNumbers != (1..EXPECTED_SURAH_COUNT).toList()) {
            failures += "xml surah numbers not 1..114: first=${surahNumbers.firstOrNull()} last=${surahNumbers.lastOrNull()}"
        }
        val basmalaCount = xmlText.surahs.count { it.basmala != null }
        if (basmalaCount != EXPECTED_SURAH_COUNT - 2) {
            failures += "xml basmala count $basmalaCount != ${EXPECTED_SURAH_COUNT - 2}"
        }
        val surah1 = xmlText.surahs.first { it.surahNumber == 1 }
        if (surah1.basmala != null) failures += "surah 1 must not carry a separate basmala"
        val surah9 = xmlText.surahs.first { it.surahNumber == 9 }
        if (surah9.basmala != null) failures += "surah 9 must not carry a basmala"
        if (surah1.nameArabic.isBlank() || surah9.nameArabic.isBlank() || surah1.ayahs.isEmpty()) {
            failures += "xml surah names/ayahs unexpectedly empty"
        }

        // per-surah numbering must be strictly 1..count
        for (s in xmlText.surahs) {
            val expected = (1..s.ayahs.size).toList()
            val actual = s.ayahs.map { it.ayahNumberInSurah }
            if (actual != expected) {
                failures += "surah ${s.surahNumber} ayah numbering broken (first=${actual.firstOrNull()} last=${actual.lastOrNull()})"
            }
        }

        val orderedAyahs = xmlText.ayahs()

        // --- plain txt stream (no numbers) --------------------------------------
        if (txtLines.size != EXPECTED_AYAH_COUNT) {
            failures += "txt line count ${txtLines.size} != $EXPECTED_AYAH_COUNT"
        }

        // --- numbered txt-2 stream ----------------------------------------------
        if (txt2.size != EXPECTED_AYAH_COUNT) {
            failures += "txt-2 count ${txt2.size} != $EXPECTED_AYAH_COUNT"
        }
        txt2.forEachIndexed { index, ayah ->
            if (ayah.surahNumber < 1 || ayah.surahNumber > EXPECTED_SURAH_COUNT) {
                failures += "txt-2 ref out of range at $index: ${ayah.surahNumber}"
            }
        }

        // --- metadata structural checks ------------------------------------------
        if (metadata.surahs.size != EXPECTED_SURAH_COUNT) {
            failures += "metadata surah count ${metadata.surahs.size} != $EXPECTED_SURAH_COUNT"
        }
        if (metadata.totalAyahCount != EXPECTED_AYAH_COUNT) {
            failures += "metadata total ${metadata.totalAyahCount} != $EXPECTED_AYAH_COUNT"
        }
        if (metadata.juz.size != EXPECTED_JUZ_COUNT) {
            failures += "metadata juz count ${metadata.juz.size} != $EXPECTED_JUZ_COUNT"
        }
        if (metadata.pages.size != EXPECTED_PAGE_COUNT) {
            failures += "metadata page count ${metadata.pages.size} != $EXPECTED_PAGE_COUNT"
        }
        if (metadata.sajdas.size != EXPECTED_SAJDA_COUNT) {
            failures += "metadata sajda count ${metadata.sajdas.size} != $EXPECTED_SAJDA_COUNT"
        }

        var runningOffset = 0
        val revelationOrders = mutableListOf<Int>()
        for (s in metadata.surahs) {
            if (s.startGlobalIndex != runningOffset) {
                failures += "surah ${s.surahNumber} start offset ${s.startGlobalIndex} != expected $runningOffset"
            }
            runningOffset += s.ayahCount
            revelationOrders += s.revelationOrder
            val xmlCount = xmlText.surahs.firstOrNull { it.surahNumber == s.surahNumber }?.ayahs?.size
            if (xmlCount != s.ayahCount) {
                failures += "surah ${s.surahNumber} xml count $xmlCount != metadata ${s.ayahCount}"
            }
        }
        if (runningOffset != EXPECTED_AYAH_COUNT) {
            failures += "metadata cumulative total $runningOffset != $EXPECTED_AYAH_COUNT"
        }
        if (revelationOrders.sorted() != (1..EXPECTED_SURAH_COUNT).toList()) {
            failures += "revelation order not a permutation of 1..114"
        }

        // monotonic boundaries
        requireMonotonic(metadata.juz.map { it.start }, "juz", failures)
        requireMonotonic(metadata.pages.map { it.start }, "page", failures)

        // --- cross-stream byte-faithfulness (txt == xml-composed display text) ---
        val displayed = orderedAyahs.map { a -> xmlText.displayText(a.surahNumber, a.ayahNumberInSurah) }
        var earliestMismatch = -1
        for (i in displayed.indices) {
            if (txtLines[i] != displayed[i]) {
                if (earliestMismatch < 0) earliestMismatch = i
            }
        }
        if (earliestMismatch >= 0) {
            failures += "txt stream differs from xml-composed text at global index $earliestMismatch"
        }
        txt2.forEachIndexed { index, ayah ->
            val expectedRef = orderedAyahs[index]
            if (ayah.surahNumber != expectedRef.surahNumber || ayah.ayahNumberInSurah != expectedRef.ayahNumberInSurah) {
                failures += "txt-2 ref mismatch at $index: ${ayah.surahNumber}:${ayah.ayahNumberInSurah} != ${expectedRef.surahNumber}:${expectedRef.ayahNumberInSurah}"
            }
            if (ayah.text != displayed[index]) {
                failures += "txt-2 text mismatch at $index"
            }
        }

        // --- text quality --------------------------------------------------------
        for (i in orderedAyahs.indices) {
            val text = displayed[i]
            if (text.isEmpty()) failures += "empty text at $i"
            if (text.contains('\u0000')) failures += "NUL byte at $i"
            if (text.contains('\uFFFD')) failures += "U+FFFD replacement at $i"
            if (!ArabicSearchNormalizer.containsConvertibleScript(text)) failures += "no Arabic script at $i"
        }

        // --- known external ground truths -----------------------------------------
        checkTruth(metadata.pages.size > 1 && metadata.pages[1].start == AyahRef(2, 1), "page 2 must start at 2:1", failures)
        checkTruth(metadata.juz.size > 1 && metadata.juz[1].start == AyahRef(2, 142), "juz 2 must start at 2:142", failures)
        checkTruth(metadata.pages.last().start == AyahRef(112, 1), "page 604 must start at 112:1", failures)
        checkTruth(metadata.sajdas.first() == AyahRef(7, 206), "first sajda must be 7:206", failures)

        if (failures.isNotEmpty()) {
            throw IllegalArgumentException("QURAN VALIDATION FAILED:\n  - " + failures.joinToString("\n  - "))
        }
    }

    private fun requireMonotonic(references: List<AyahRef>, kind: String, failures: MutableList<String>) {
        for (i in 1 until references.size) {
            if (references[i] <= references[i - 1]) {
                failures += "$kind boundary not strictly increasing at index $i"
                return
            }
        }
    }

    private fun checkTruth(condition: Boolean, message: String, failures: MutableList<String>) {
        if (!condition) failures += message
    }
}