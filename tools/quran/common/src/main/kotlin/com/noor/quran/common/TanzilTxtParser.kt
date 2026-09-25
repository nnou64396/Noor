package com.noor.quran.common

import java.io.File

/**
 * Parses the Tanzil Uthmani plain-text export (`outType=txt`), which has NO ayah numbers:
 * one ayah per line (basmala merged into ayah 1), with a `#` comment/license header.
 * Returns the ordered ayah texts (6236 expected). Blank/comment lines are skipped.
 */
object TanzilTxtParser {

    fun parse(file: File): List<String> {
        val lines = mutableListOf<String>()
        file.forEachLine { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachLine
            if (line.startsWith("#")) return@forEachLine
            lines += line
        }
        return lines
    }
}