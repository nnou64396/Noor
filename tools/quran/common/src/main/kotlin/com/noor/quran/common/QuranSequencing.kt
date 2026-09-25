package com.noor.quran.common

/**
 * Derives per-ayah attributes (global number, juz, page, sajdah flag) from the canonical
 * ordered ayah stream and the metadata ranges. The ayah stream must already be in
 * canonical global order (surah ascending, then ayah within surah).
 */
object QuranSequencing {

    fun computeAttributes(
        ayahsInGlobalOrder: List<QuranAyah>,
        metadata: QuranMetadata,
    ): List<AyahAttributes> {
        val sajdaSet = metadata.sajdas.toSet()
        val juz = metadata.juz.sortedBy { it.juzNumber }
        val pages = metadata.pages.sortedBy { it.pageNumber }

        var juzIdx = 0
        var pageIdx = 0

        return ayahsInGlobalOrder.mapIndexed { index, ayah ->
            val ref = AyahRef(ayah.surahNumber, ayah.ayahNumberInSurah)
            while (juzIdx + 1 < juz.size && ref >= juz[juzIdx + 1].start) {
                juzIdx++
            }
            while (pageIdx + 1 < pages.size && ref >= pages[pageIdx + 1].start) {
                pageIdx++
            }
            AyahAttributes(
                globalAyahNumber = index + 1,
                surahNumber = ayah.surahNumber,
                ayahNumberInSurah = ayah.ayahNumberInSurah,
                juz = juz.getOrNull(juzIdx)?.juzNumber ?: 0,
                pageNumber = pages.getOrNull(pageIdx)?.pageNumber ?: 0,
                hasSajdah = ref in sajdaSet,
            )
        }
    }

    /** Page on which each surah starts, keyed by surah number (page of that surah's first ayah). */
    fun surahStartPages(attributes: List<AyahAttributes>): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (attr in attributes) {
            if (attr.surahNumber !in result) {
                result[attr.surahNumber] = attr.pageNumber
            }
        }
        return result
    }
}