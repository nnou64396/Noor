package com.noor.islamic.domain.model

/**
 * Prayer-time calculation methods supported by the AlAdhan API.
 *
 * The numeric ids follow the official AlAdhan method reference. This is a
 * domain/config representation only — it is not presented as a religious
 * ruling, and it can be swapped later from Settings without rewriting the
 * prayer feature.
 */
enum class CalculationMethod(val apiId: Int) {
    UNIVERSITY_OF_KARACHI(1),
    ISNA(2),
    MUSLIM_WORLD_LEAGUE(3),
    UMM_AL_QURA(4),
    EGYPTIAN_GENERAL_AUTHORITY(5),
    TEHRAN(7),
    GULF_REGION(8),
    KUWAIT(9),
    QATAR(10),
    SINGAPORE(11),
    FRENCH_MUSLIMS(12),
    TURKEY(13),
    RUSSIA(14),
    MOONSIGHTING_COMMITTEE(15),
    DUBAI(16),
    JAKARTA(17),
}

/**
 * Configuration knobs for the prayer feature.
 *
 * Default for Noor Phase 2 is the Muslim World League (MWL) method, a
 * common choice for the Maghreb region. The Fiqh school stays at the
 * standard (Shafi'i/Salafi) Asr timing. Both remain internal here; a
 * Settings entry for them belongs to a later phase.
 */
object CalculationMethodConfig {
    val defaultMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE

    /** AlAdhan `school` param: 0 = Standard (Shafi'i), 1 = Hanafi. */
    const val defaultSchool = 0
}