package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.GeographicPoint
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.DailyPrayerSchedule

/**
 * Thin abstraction over location permission checks so ViewModels stay free
 * of Android framework calls.
 */
interface LocationPermissionChecker {
    fun isGranted(): Boolean

    /** True when a rationale dialog is still useful (denial was not permanent). */
    fun shouldShowRationale(): Boolean
}
