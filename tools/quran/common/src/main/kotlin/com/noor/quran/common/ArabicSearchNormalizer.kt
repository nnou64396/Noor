package com.noor.quran.common

/**
 * Deterministic Arabic normalization used ONLY for the FTS search column.
 * Never applied to display text. Rules:
 *  - strip tashkeel U+064B..U+065F and the dagger-alef U+0670
 *  - strip tatweel U+0640
 *  - unify hamza-bearing letters to their plain base: آ أ إ ٱ -> ا (0627), ؤ ئ -> ء (0621)
 *  - unify alef maqsura ى -> ي (064A)
 */
object ArabicSearchNormalizer {

    private const val BASE_ALEF = 0x0627
    private const val HAMZA = 0x0621
    private const val YEH = 0x064A

    private fun normalizeCodePoint(cp: Int): Int = when (cp) {
        0x0640 -> -1 // tatweel
        in 0x064B..0x065F -> -1 // marks
        0x0670 -> -1 // dagger alef
        0x0622, 0x0623, 0x0625, 0x0671 -> BASE_ALEF
        0x0624, 0x0626 -> HAMZA
        0x0649 -> YEH
        else -> cp
    }

    /** Applies normalization to a display string. Pure function; the input is never modified. */
    fun normalize(input: String): String {
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            i += Character.charCount(cp)
            val mapped = normalizeCodePoint(cp)
            if (mapped >= 0) out.appendCodePoint(mapped)
        }
        return out.toString()
    }

    fun containsConvertibleScript(input: String): Boolean =
        input.any { it.code in 0x0600..0x06FF }
}