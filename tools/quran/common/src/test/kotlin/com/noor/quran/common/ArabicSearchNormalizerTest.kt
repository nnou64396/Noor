package com.noor.quran.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicSearchNormalizerTest {

    @Test
    fun stripsTashkeel() {
        assertEquals("بسم", ArabicSearchNormalizer.normalize("بِسْمِ"))
    }

    @Test
    fun stripsTatweel() {
        assertEquals("قال", ArabicSearchNormalizer.normalize("قا\u0640\u0640ل"))
    }

    @Test
    fun stripsDaggerAlef() {
        assertEquals("الرحمن", ArabicSearchNormalizer.normalize("الرَّحْمَٰن"))
    }

    @Test
    fun unifiesHamzaAlefsToAlef() {
        assertEquals("القران", ArabicSearchNormalizer.normalize("ٱلْقُرْآن"))
        assertEquals("ااا", ArabicSearchNormalizer.normalize("أآإ"))
        assertEquals("اا", ArabicSearchNormalizer.normalize("ٱا"))
    }

    @Test
    fun unifiesHamzaOnYehAndWao() {
        assertEquals("ء", ArabicSearchNormalizer.normalize("ئ"))
        assertEquals("ء", ArabicSearchNormalizer.normalize("ؤ"))
    }

    @Test
    fun unifiesAlefMaqsuraToYeh() {
        assertEquals("ي", ArabicSearchNormalizer.normalize("ى"))
        assertEquals("يحيي", ArabicSearchNormalizer.normalize("يحيى"))
    }

    @Test
    fun inputWithoutMarksIsUnchanged() {
        val s = "بسم الله الرحمن الرحيم"
        assertEquals(s, ArabicSearchNormalizer.normalize(s))
    }

    @Test
    fun containsConvertibleScriptDetectsArabic() {
        assertTrue(ArabicSearchNormalizer.containsConvertibleScript("بسم الله"))
        assertFalse(ArabicSearchNormalizer.containsConvertibleScript("ABC 123"))
    }
}