package com.noor.islamic.domain.model

import androidx.annotation.StringRes
import com.noor.islamic.R

/**
 * Prayer-time calculation methods supported by the AlAdhan API.
 *
 * The numeric ids follow the official AlAdhan method reference. This is a
 * domain/config representation only — it is not presented as a religious
 * ruling, and it can be swapped later from Settings without rewriting the
 * prayer feature.
 */
enum class CalculationMethod(
    val apiId: Int,
    val storageKey: String,
    @StringRes val displayNameRes: Int,
) {
    UNIVERSITY_OF_KARACHI(1, "university_of_karachi", R.string.calculation_method_karachi),
    ISNA(2, "isna", R.string.calculation_method_isna),
    MUSLIM_WORLD_LEAGUE(3, "muslim_world_league", R.string.calculation_method_mwl),
    UMM_AL_QURA(4, "umm_al_qura", R.string.calculation_method_umm_al_qura),
    EGYPTIAN_GENERAL_AUTHORITY(5, "egyptian_general_authority", R.string.calculation_method_egyptian),
    TEHRAN(7, "tehran", R.string.calculation_method_tehran),
    GULF_REGION(8, "gulf_region", R.string.calculation_method_gulf),
    KUWAIT(9, "kuwait", R.string.calculation_method_kuwait),
    QATAR(10, "qatar", R.string.calculation_method_qatar),
    SINGAPORE(11, "singapore", R.string.calculation_method_singapore),
    FRENCH_MUSLIMS(12, "french_muslims", R.string.calculation_method_french),
    TURKEY(13, "turkey", R.string.calculation_method_turkey),
    RUSSIA(14, "russia", R.string.calculation_method_russia),
    MOONSIGHTING_COMMITTEE(15, "moonsighting_committee", R.string.calculation_method_moonsighting),
    DUBAI(16, "dubai", R.string.calculation_method_dubai),
    JAKARTA(17, "jakarta", R.string.calculation_method_jakarta),
    ;

    companion object {
        val DEFAULT: CalculationMethod = MUSLIM_WORLD_LEAGUE

        fun fromStorageKey(key: String?): CalculationMethod =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT

        fun fromApiId(apiId: Int?): CalculationMethod =
            entries.firstOrNull { it.apiId == apiId } ?: DEFAULT
    }
}

enum class AsrSchool(
    val apiId: Int,
    val storageKey: String,
    @StringRes val displayNameRes: Int,
) {
    STANDARD(0, "standard", R.string.asr_school_standard),
    HANAFI(1, "hanafi", R.string.asr_school_hanafi),
    ;

    companion object {
        val DEFAULT: AsrSchool = STANDARD

        fun fromStorageKey(key: String?): AsrSchool =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT

        fun fromApiId(apiId: Int?): AsrSchool =
            entries.firstOrNull { it.apiId == apiId } ?: DEFAULT
    }
}

enum class LocationMode(
    val storageKey: String,
    @StringRes val displayNameRes: Int,
) {
    AUTO("auto", R.string.location_mode_auto),
    MANUAL("manual", R.string.location_mode_manual),
    ;

    companion object {
        val DEFAULT: LocationMode = AUTO

        fun fromStorageKey(key: String?): LocationMode =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/**
 * Configuration knobs for the prayer feature.
 *
 * Default for Noor Phase 2 is the Muslim World League (MWL) method, a
 * common choice for the Maghreb region. The Fiqh school stays at the
 * standard (Shafi'i/Salafi) Asr timing.
 */
object CalculationMethodConfig {
    val defaultMethod: CalculationMethod = CalculationMethod.DEFAULT

    /** AlAdhan `school` param: 0 = Standard (Shafi'i), 1 = Hanafi. */
    val defaultSchool: Int = AsrSchool.DEFAULT.apiId
}