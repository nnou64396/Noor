package com.noor.islamic.data.local.db.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Surah metadata header. Prebuilt offline DB `quran_reader.db`; read-only.
 * `basmala` is the surah's header basmala (null for surah 1 - it is 1:1 - and surah 9).
 */
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
    @ColumnInfo(name = "revelation_order") val revelationOrder: Int,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "start_page") val startPage: Int,
    @ColumnInfo(name = "basmala") val basmala: String?,
)