package dz.techvision.noor.domain.usecase

import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Exposes the current [AppSettings] as a single observable stream.
 */
private data class SettingsState(
    val language: Language,
    val themeMode: ThemeMode,
    val onboardingComplete: Boolean,
    val calculationMethod: CalculationMethod = CalculationMethod.DEFAULT,
    val asrSchool: AsrSchool = AsrSchool.DEFAULT,
    val locationMode: LocationMode = LocationMode.DEFAULT,
    val manualLatitude: Double? = null,
    val manualLongitude: Double? = null,
)

class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> =
        combine(
            repository.language,
            repository.themeMode,
            repository.onboardingComplete,
        ) { language, themeMode, onboardingComplete ->
            SettingsState(
                language = language,
                themeMode = themeMode,
                onboardingComplete = onboardingComplete,
            )
        }
            .combine(repository.prayerCalculationMethod) { state, calculationMethod ->
                state.copy(calculationMethod = calculationMethod)
            }
            .combine(repository.prayerAsrSchool) { state, asrSchool ->
                state.copy(asrSchool = asrSchool)
            }
            .combine(repository.prayerLocationMode) { state, locationMode ->
                state.copy(locationMode = locationMode)
            }
            .combine(repository.prayerManualLatitude) { state, manualLatitude ->
                state.copy(manualLatitude = manualLatitude)
            }
            .combine(repository.prayerManualLongitude) { state, manualLongitude ->
                state.copy(manualLongitude = manualLongitude)
            }
            .map { state ->
                AppSettings(
                    language = state.language,
                    themeMode = state.themeMode,
                    onboardingComplete = state.onboardingComplete,
                    calculationMethod = state.calculationMethod,
                    asrSchool = state.asrSchool,
                    locationMode = state.locationMode,
                    manualLatitude = state.manualLatitude,
                    manualLongitude = state.manualLongitude,
                )
            }
}
