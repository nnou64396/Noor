package dz.techvision.noor.domain.model

/**
 * Aggregate of the user preferences and flags that shape the app.
 */
data class AppSettings(
    val language: Language = Language.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val onboardingComplete: Boolean = false,
    val calculationMethod: CalculationMethod = CalculationMethodConfig.defaultMethod,
    val asrSchool: AsrSchool = AsrSchool.DEFAULT,
    val locationMode: LocationMode = LocationMode.DEFAULT,
    val manualLatitude: Double? = null,
    val manualLongitude: Double? = null,
    val notificationsEnabled: Boolean = true,
    val prayerRemindersEnabled: Boolean = true,
    val updateNotificationsEnabled: Boolean = true,
)
