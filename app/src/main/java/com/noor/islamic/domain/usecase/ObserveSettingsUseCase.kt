package com.noor.islamic.domain.usecase

import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Exposes the current [AppSettings] as a single observable stream.
 */
class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> =
        combine(
            repository.language,
            repository.themeMode,
            repository.onboardingComplete,
        ) { language, themeMode, onboardingComplete ->
            AppSettings(
                language = language,
                themeMode = themeMode,
                onboardingComplete = onboardingComplete,
            )
        }
}