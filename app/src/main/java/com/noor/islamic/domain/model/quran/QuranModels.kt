package com.noor.islamic.domain.model.quran

enum class RevelationType { MECCAN, MEDINAN }

data class Surah(
    val surahNumber: Int,
    val nameArabic: String,
    val nameTransliteration: String,
    val nameEnglish: String,
    val ayahCount: Int,
    val revelationOrder: Int,
    val revelationType: RevelationType,
    val startPage: Int,
    val basmala: String?,
)

data class Ayah(
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val juzNumber: Int,
    val pageNumber: Int,
    val hasSajdah: Boolean,
    val textUthmani: String,
)