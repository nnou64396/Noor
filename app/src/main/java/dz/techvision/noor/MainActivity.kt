package dz.techvision.noor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.data.local.location.AndroidLocationPermissionChecker
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.navigation.NoorApp
import dz.techvision.noor.presentation.MainViewModel
import dz.techvision.noor.presentation.SettingsUiState
import dz.techvision.noor.presentation.prayer.PrayerViewModel
import dz.techvision.noor.services.NotificationChannels
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.theme.NoorTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize notification channels for Android 8.0+
        NotificationChannels.createChannels(this)

        setContent {
            val viewModel: MainViewModel = viewModel {
                MainViewModel(
                    repository = ServiceLocator.settingsRepository,
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

            val themeMode = (settingsState as? SettingsUiState.Ready)
                ?.settings?.themeMode
                ?: ThemeMode.DEFAULT
            NoorTheme(themeMode = themeMode) {
                NoorApp(
                    settingsState = settingsState,
                    prayerViewModel = prayerViewModel,
                    onLanguageSelected = viewModel::setLanguage,
                    onThemeSelected = viewModel::setThemeMode,
                    onCalculationMethodSelected = viewModel::setPrayerCalculationMethod,
                    onAsrSchoolSelected = viewModel::setPrayerAsrSchool,
                    onLocationModeSelected = viewModel::setPrayerLocationMode,
                    onManualLocationSelected = viewModel::setPrayerManualLocation,
                    onNotificationsEnabledChanged = viewModel::setNotificationsEnabled,
                    onPrayerRemindersChanged = viewModel::setPrayerRemindersEnabled,
                    onOnboardingComplete = viewModel::completeOnboarding,
                )
            }
        }
    }
}
