package dz.techvision.noor.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.presentation.SettingsUiState
import dz.techvision.noor.presentation.prayer.PrayerViewModel
import dz.techvision.noor.ui.components.NoorBottomBar

@Composable
fun NoorApp(
    settingsState: SettingsUiState,
    prayerViewModel: PrayerViewModel,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onCalculationMethodSelected: (CalculationMethod) -> Unit = {},
    onAsrSchoolSelected: (AsrSchool) -> Unit = {},
    onLocationModeSelected: (LocationMode) -> Unit = {},
    onManualLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    onNotificationsEnabledChanged: (Boolean) -> Unit = {},
    onPrayerRemindersChanged: (Boolean) -> Unit = {},
    onOnboardingComplete: () -> Unit,
) {
    val settings: AppSettings = when (val state = settingsState) {
        SettingsUiState.Loading -> return
        is SettingsUiState.Ready -> state.settings
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showsBottomBar = currentRoute in NoorTab.allRoutes

    val startDestination =
        if (settings.onboardingComplete) NoorDestinations.HOME
        else NoorDestinations.ONBOARDING

    Scaffold(
        bottomBar = {
            if (showsBottomBar) {
                NoorBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NoorNavHost(
            navController = navController,
            startDestination = startDestination,
            contentPadding = innerPadding,
            settings = settings,
            prayerViewModel = prayerViewModel,
            onLanguageSelected = onLanguageSelected,
            onThemeSelected = onThemeSelected,
            onCalculationMethodSelected = onCalculationMethodSelected,
            onAsrSchoolSelected = onAsrSchoolSelected,
            onLocationModeSelected = onLocationModeSelected,
            onManualLocationSelected = onManualLocationSelected,
            onNotificationsEnabledChanged = onNotificationsEnabledChanged,
            onPrayerRemindersChanged = onPrayerRemindersChanged,
            onOpenDestination = { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                }
            },
            onOnboardingComplete = {
                onOnboardingComplete()
                navController.navigate(NoorDestinations.HOME) {
                    launchSingleTop = true
                    popUpTo(NoorDestinations.ONBOARDING) { inclusive = true }
                }
            },
        )
    }
}
