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
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.ui.components.NoorBottomBar

/**
 * Root composable: holds the [Scaffold] and wires the app destinations
 * together with the branded bottom navigation bar.
 */
@Composable
fun NoorApp(
    settings: AppSettings,
    prayerViewModel: PrayerViewModel,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onOnboardingComplete: () -> Unit,
) {
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
            onOpenDestination = { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                }
            },
            onOnboardingComplete = {
                onOnboardingComplete()
                navController.navigate(NoorDestinations.HOME) {
                    popUpTo(NoorDestinations.ONBOARDING) { inclusive = true }
                }
            },
        )
    }
}