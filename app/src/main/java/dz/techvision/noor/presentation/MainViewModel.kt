package dz.techvision.noor.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.domain.repository.SettingsRepository
import dz.techvision.noor.domain.repository.UpdateRepository
import dz.techvision.noor.domain.usecase.ObserveSettingsUseCase
import dz.techvision.noor.utils.LocaleManager
import dz.techvision.noor.utils.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether persisted settings are available yet.
 *
 * The navigation graph is gated on [SettingsUiState.Ready] so the start
 * destination is always decided from the stored value: a returning user must
 * never see even a single frame of Onboarding, and the first frame of the
 * graph is always built with the real start destination.
 */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: AppSettings) : SettingsUiState
}

class MainViewModel(
    private val repository: SettingsRepository,
    private val updateRepository: UpdateRepository? = null,
) : ViewModel() {

    private val observeSettings = ObserveSettingsUseCase(repository)

    val updateStatus: StateFlow<UpdateStatus> = updateRepository?.updateStatus
        ?: MutableStateFlow(UpdateStatus.UpToDate)

    init {
        checkForUpdates()
    }

    fun checkForUpdates(forceCheck: Boolean = false) {
        val updateRepo = updateRepository ?: return
        viewModelScope.launch {
            updateRepo.checkForUpdates(forceCheck = forceCheck)
        }
    }

    fun dismissUpdate(versionCode: Long) {
        val updateRepo = updateRepository ?: return
        viewModelScope.launch {
            updateRepo.dismissUpdate(versionCode)
        }
    }

    fun downloadAndVerifyUpdate(info: AppUpdateInfo) {
        val updateRepo = updateRepository ?: return
        viewModelScope.launch {
            updateRepo.downloadAndVerifyUpdate(info)
        }
    }

    fun retryUpdate(info: AppUpdateInfo, context: Context) {
        val updateRepo = updateRepository ?: return
        viewModelScope.launch {
            updateRepo.retryUpdate(info, context)
        }
    }

    fun installUpdate(context: Context) {
        val updateRepo = updateRepository ?: return
        updateRepo.installUpdate(context)
    }

    /**
     * Called after returning from the unknown-app-sources settings screen.
     * When permission was granted, installation resumes automatically.
     */
    fun onInstallPermissionResult(context: Context): Boolean =
        updateRepository?.onInstallPermissionResult(context) ?: false

    /** [SettingsUiState.Ready] as soon as the store answers; [SettingsUiState.Loading] until then. */
    val settingsState: StateFlow<SettingsUiState> = observeSettings()
        .map { SettingsUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SettingsUiState.Loading,
        )

    fun setLanguage(language: Language) {
        LocaleManager.apply(language)
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        ThemeManager.apply(themeMode)
        viewModelScope.launch { repository.setThemeMode(themeMode) }
    }

    fun setPrayerCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch { repository.setPrayerCalculationMethod(method) }
    }

    fun setPrayerAsrSchool(school: AsrSchool) {
        viewModelScope.launch { repository.setPrayerAsrSchool(school) }
    }

    fun setPrayerLocationMode(mode: LocationMode) {
        viewModelScope.launch { repository.setPrayerLocationMode(mode) }
    }

    fun setPrayerManualLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch { repository.setPrayerManualLocation(latitude, longitude) }
    }

    fun clearPrayerManualLocation() {
        viewModelScope.launch { repository.clearPrayerManualLocation() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingComplete(true) }
    }
}
