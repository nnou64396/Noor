package com.noor.quran.common

enum class RevelationType { MECCAN, MEDINAN }

data class QuranAyah(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val text: String,
)

data class SurahText(
    val surahNumber: Int,
    val nameArabic: String,
    val basmala: String?,
    val ayahs: List<QuranAyah>,
) {
    companion object {
        /** Surah 1 carries the basmala as its first ayah; surah 9 has none. */
        fun hasSeparateBasmala(surahNumber: Int) = surahNumber != 1 && surahNumber != 9
    }
}

data class QuranText(
    val surahs: List<SurahText>,
) {
    val totalAyahs: Int get() = surahs.sumOf { it.ayahs.size }
    fun ayahs(): List<QuranAyah> = surahs.flatMap { it.ayahs }

    /** The full display text of an ayah as it appears in the alphabetized txt streams
     *  (basmala merged into ayah 1 for every surah except 1 and 9). */
    fun displayText(surahNumber: Int, ayahNumberInSurah: Int): String {
        val surah = surahs.first { it.surahNumber == surahNumber }
        val ayah = surah.ayahs.first { it.ayahNumberInSurah == ayahNumberInSurah }
        val basmala = surah.basmala
        return if (ayahNumberInSurah == 1 && basmala != null) "$basmala ${ayah.text}" else ayah.text
    }
}

data class SurahMeta(
    val surahNumber: Int,
    val ayahCount: Int,
    val startGlobalIndex: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameEnglish: String,
    val revelationType: RevelationType,
    val revelationOrder: Int,
    val rukus: Int,
) {
    val endExclusiveGlobalIndex: Int get() = startGlobalIndex + ayahCount
}

data class AyahRef(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
) : Comparable<AyahRef> {
    override fun compareTo(other: AyahRef): Int {
        val s = surahNumber.compareTo(other.surahNumber)
        return if (s != 0) s else ayahNumberInSurah.compareTo(other.ayahNumberInSurah)
    }
}

data class JuzBoundary(
    val juzNumber: Int,
    val start: AyahRef,
)

data class PageBoundary(
    val pageNumber: Int,
    val start: AyahRef,
)

data class QuranMetadata(
    val surahs: List<SurahMeta>,
    val juz: List<JuzBoundary>,
    val pages: List<PageBoundary>,
    val sajdas: List<AyahRef>,
) {
    val totalAyahCount: Int get() = surahs.sumOf { it.ayahCount }
}

data class AyahAttributes(
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val juz: Int,
    val pageNumber: Int,
    val hasSajdah: Boolean,
)