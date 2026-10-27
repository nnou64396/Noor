package dz.techvision.noor

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dz.techvision.noor.data.local.location.AndroidLocationPermissionChecker
import dz.techvision.noor.data.update.ApkInstaller
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.navigation.NoorApp
import dz.techvision.noor.presentation.MainViewModel
import dz.techvision.noor.presentation.SettingsUiState
import dz.techvision.noor.presentation.prayer.PrayerViewModel
import dz.techvision.noor.services.ServiceLocator
import dz.techvision.noor.ui.theme.NoorTheme

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

            val installPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) {
                viewModel.onInstallPermissionResult(this@MainActivity)
            }

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
                    onDownloadAndVerifyUpdate = viewModel::downloadAndVerifyUpdate,
                    onInstallUpdate = viewModel::installUpdate,
                    onRetryUpdate = viewModel::retryUpdate,
                    onOpenInstallSettings = {
                        val settingsIntent =
                            ApkInstaller.buildInstallPermissionSettingsIntent(this@MainActivity)
                        runCatching { installPermissionLauncher.launch(settingsIntent) }
                            .onFailure {
                                runCatching {
                                    installPermissionLauncher.launch(
                                        ApkInstaller.buildAppDetailsSettingsIntent(this@MainActivity),
                                    )
                                }
                            }
                    },
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
