package com.noor.islamic.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMappingTest {

    @Test
    fun language_fromCode_defaultsToArabic() {
        assertEquals(Language.ARABIC, Language.fromCode(null))
        assertEquals(Language.ARABIC, Language.fromCode("unknown"))
    }

    @Test
    fun language_fromCode_mapsKnownCodes() {
        assertEquals(Language.ARABIC, Language.fromCode("ar"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.FRENCH, Language.fromCode("fr"))
    }

    @Test
    fun themeMode_fromStorageKey_defaultsToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("unknown"))
    }

    @Test
    fun themeMode_fromStorageKey_mapsKnownKeys() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("dark"))
    }
}