package com.noor.islamic.domain.model.quran

/**
 * Stable identity of one ayah inside the Quran corpus.
 *
 * Bookmarks and reading progress are keyed on these canonical numbers, never on the
 * displayed Arabic text, so a bookmark survives any presentation change.
 */
data class AyahRef(
    val surahNumber: Int,
    val ayahNumberInSurah: Int,
    val globalAyahNumber: Int,
) {
    companion object {
        fun of(ayah: Ayah): AyahRef = AyahRef(
            surahNumber = ayah.surahNumber,
            ayahNumberInSurah = ayah.ayahNumberInSurah,
            globalAyahNumber = ayah.globalAyahNumber,
        )
    }
}

/** A locally saved bookmark of a single ayah. */
data class Bookmark(
    val ref: AyahRef,
    val createdAt: Long,
)

/** The most recent local reading position. */
data class ReadingProgress(
    val ref: AyahRef,
    val updatedAt: Long,
)
