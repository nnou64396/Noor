package com.noor.quran.importer

import com.noor.quran.common.ArabicSearchNormalizer
import com.noor.quran.common.AyahAttributes
import com.noor.quran.common.QuranMetadata
import com.noor.quran.common.QuranText

data class RowSets(
    val surahs: List<Map<String, Any?>>,
    val ayahs: List<Map<String, Any?>>,
)

/**
 * Builds the exact row maps the app's Room entities expect. Column keys are the
 * snake_case names pinned by `@ColumnInfo` in [com.noor.islamic.data.local.db.quran]'s
 * entities; [Main.assertColumnContract] fails the build if they ever drift apart.
 */
internal fun buildSurahRows(
    xml: QuranText,
    metadata: QuranMetadata,
    startPages: Map<Int, Int>,
): List<Map<String, Any?>> = xml.surahs.map { s ->
    val meta = metadata.surahs.first { it.surahNumber == s.surahNumber }
    linkedMapOf<String, Any?>(
        "surah_number" to s.surahNumber,
        "name_arabic" to s.nameArabic,
        "name_transliteration" to meta.nameTransliteration,
        "name_english" to meta.nameEnglish,
        "ayah_count" to s.ayahs.size,
        "revelation_order" to meta.revelationOrder,
        "revelation_type" to meta.revelationType.name,
        "start_page" to startPages.getValue(s.surahNumber),
        "basmala" to s.basmala,
    )
}

internal fun buildAyahRows(
    ordered: List<com.noor.quran.common.QuranAyah>,
    attributes: List<AyahAttributes>,
    xml: QuranText,
): List<Map<String, Any?>> = ordered.mapIndexed { index, ayah ->
    val at = attributes[index]
    val display = xml.displayText(ayah.surahNumber, ayah.ayahNumberInSurah)
    linkedMapOf<String, Any?>(
        "global_ayah_number" to at.globalAyahNumber,
        "surah_number" to ayah.surahNumber,
        "ayah_number_in_surah" to ayah.ayahNumberInSurah,
        "juz_number" to at.juz,
        "page_number" to at.pageNumber,
        "has_sajdah" to at.hasSajdah,
        "text_uthmani" to display,
        "text_search" to ArabicSearchNormalizer.normalize(display),
    )
}