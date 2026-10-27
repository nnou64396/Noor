package dz.techvision.noor.domain.model

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

    @Test
    fun prayerSettings_defaultToExistingCalculationAndAutomaticLocation() {
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.fromStorageKey(null))
        assertEquals(AsrSchool.STANDARD, AsrSchool.fromStorageKey(null))
        assertEquals(LocationMode.AUTO, LocationMode.fromStorageKey(null))
    }

    @Test
    fun prayerSettings_unknownValuesFallBackToDefaults() {
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.fromStorageKey("invalid"))
        assertEquals(AsrSchool.STANDARD, AsrSchool.fromStorageKey("invalid"))
        assertEquals(LocationMode.AUTO, LocationMode.fromStorageKey("invalid"))
    }

    @Test
    fun prayerSettings_storageKeysRestoreSelectedValues() {
        assertEquals(CalculationMethod.TURKEY, CalculationMethod.fromStorageKey("turkey"))
        assertEquals(AsrSchool.HANAFI, AsrSchool.fromStorageKey("hanafi"))
        assertEquals(LocationMode.MANUAL, LocationMode.fromStorageKey("manual"))
    }
}
