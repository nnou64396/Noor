package com.noor.quran.common

import java.io.File

/**
 * Parses the Tanzil Uthmani numbered plain-text export (`outType=txt-2`, `sura|ayah|text`).
 * The basmala is merged into ayah 1 (matching the `txt` stream). Blank/comment lines are skipped.
 */
object TanzilTxt2Parser {

    fun parse(file: File): List<QuranAyah> {
        val ayahs = mutableListOf<QuranAyah>()
        file.forEachLine { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachLine
            if (line.startsWith("#")) return@forEachLine
            val parts = line.split('|')
            require(parts.size == 3) { "malformed txt-2 line: $line" }
            ayahs += QuranAyah(
                surahNumber = parts[0].toInt(),
                ayahNumberInSurah = parts[1].toInt(),
                text = parts[2],
            )
        }
        return ayahs
    }
}