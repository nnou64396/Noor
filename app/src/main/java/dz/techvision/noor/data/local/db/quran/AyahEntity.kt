package dz.techvision.noor.data.local.db.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One numbered ayah. `textUthmani` is the byte-faithful Tanzil Uthmani display text
 * (basmala merged into ayah 1). `textSearch` is the search-only normalized column.
 */
@Entity(
    tableName = "ayahs",
    indices = [Index(value = ["surah_number", "ayah_number_in_surah"], unique = true)],
)
data class AyahEntity(
    @PrimaryKey @ColumnInfo(name = "global_ayah_number") val globalAyahNumber: Int,
    @ColumnInfo(name = "surah_number") val surahNumber: Int,
    @ColumnInfo(name = "ayah_number_in_surah") val ayahNumberInSurah: Int,
    @ColumnInfo(name = "juz_number") val juzNumber: Int,
    @ColumnInfo(name = "page_number") val pageNumber: Int,
    @ColumnInfo(name = "has_sajdah") val hasSajdah: Boolean,
    @ColumnInfo(name = "text_uthmani") val textUthmani: String,
    @ColumnInfo(name = "text_search") val textSearch: String,
)

/** FTS4 index over the normalized search column. */
@Fts4(contentEntity = AyahEntity::class)
@Entity(tableName = "ayahs_fts")
data class AyahFtsEntity(
    @ColumnInfo(name = "text_search") val textSearch: String,
)
