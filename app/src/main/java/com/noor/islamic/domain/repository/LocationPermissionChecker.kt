package com.noor.islamic.domain.repository

import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.DailyPrayerSchedule

/**
 * Thin abstraction over location permission checks so ViewModels stay free
 * of Android framework calls.
 */
interface LocationPermissionChecker {
    fun isGranted(): Boolean

    /** True when a rationale dialog is still useful (denial was not permanent). */
    fun shouldShowRationale(): Boolean
}