package com.noor.islamic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noor.islamic.data.local.location.AndroidLocationPermissionChecker
import com.noor.islamic.navigation.NoorApp
import com.noor.islamic.presentation.MainViewModel
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.services.ServiceLocator
import com.noor.islamic.ui.theme.NoorTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = viewModel {
                MainViewModel(ServiceLocator.settingsRepository)
            }
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            val prayerViewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    permissionChecker = AndroidLocationPermissionChecker(this@MainActivity),
                    locationRepository = ServiceLocator.locationRepository,
                    prayerRepository = ServiceLocator.prayerRepository,
                )
            }

            NoorTheme(themeMode = settings.themeMode) {
                NoorApp(
                    settings = settings,
                    prayerViewModel = prayerViewModel,
                    onLanguageSelected = viewModel::setLanguage,
                    onThemeSelected = viewModel::setThemeMode,
                    onOnboardingComplete = viewModel::completeOnboarding,
                )
            }
        }
    }
}