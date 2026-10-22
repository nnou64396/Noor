package com.noor.islamic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noor.islamic.data.local.location.AndroidLocationPermissionChecker
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.navigation.NoorApp
import com.noor.islamic.presentation.MainViewModel
import com.noor.islamic.presentation.SettingsUiState
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.services.ServiceLocator
import com.noor.islamic.ui.theme.NoorTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = viewModel {
                MainViewModel(
                    repository = ServiceLocator.settingsRepository,
                    updateRepository = ServiceLocator.updateRepository,
                )
            }
            val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

            val prayerViewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    permissionChecker = AndroidLocationPermissionChecker(this@MainActivity),
                    locationRepository = ServiceLocator.locationRepository,
                    prayerRepository = ServiceLocator.prayerRepository,
                    settingsRepository = ServiceLocator.settingsRepository,
                )
            }

            val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()

            val themeMode = (settingsState as? SettingsUiState.Ready)
                ?.settings?.themeMode
                ?: ThemeMode.DEFAULT
            NoorTheme(themeMode = themeMode) {
                NoorApp(
                    settingsState = settingsState,
                    prayerViewModel = prayerViewModel,
                    updateStatus = updateStatus,
                    onCheckForUpdates = { viewModel.checkForUpdates(forceCheck = true) },
                    onDismissUpdate = viewModel::dismissUpdate,
                    onLanguageSelected = viewModel::setLanguage,
                    onThemeSelected = viewModel::setThemeMode,
                    onCalculationMethodSelected = viewModel::setPrayerCalculationMethod,
                    onAsrSchoolSelected = viewModel::setPrayerAsrSchool,
                    onLocationModeSelected = viewModel::setPrayerLocationMode,
                    onManualLocationSelected = viewModel::setPrayerManualLocation,
                    onOnboardingComplete = viewModel::completeOnboarding,
                )
            }
        }
    }
}