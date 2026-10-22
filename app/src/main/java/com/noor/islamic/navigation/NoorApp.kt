package com.noor.islamic.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.LocationMode
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.presentation.SettingsUiState
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.ui.components.NoorBottomBar

/**
 * Root composable: holds the [Scaffold] and wires the app destinations
 * together with the branded bottom navigation bar.
 *
 * The whole subtree is gated on [SettingsUiState.Ready]: until persisted
 * settings are available the start destination cannot be decided reliably, so
 * rendering the graph early would flash Onboarding to returning users.
 */
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