package com.noor.islamic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.domain.usecase.ObserveSettingsUseCase
import com.noor.islamic.utils.LocaleManager
import com.noor.islamic.utils.ThemeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val observeSettings = ObserveSettingsUseCase(repository)

    val settings: StateFlow<AppSettings> = observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = AppSettings(),
        )

    fun setLanguage(language: Language) {
        LocaleManager.apply(language)
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        ThemeManager.apply(themeMode)
        viewModelScope.launch { repository.setThemeMode(themeMode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingComplete(true) }
    }
}