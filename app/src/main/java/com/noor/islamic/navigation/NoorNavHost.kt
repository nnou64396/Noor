package com.noor.islamic.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.presentation.athkar.AthkarScreen
import com.noor.islamic.presentation.home.HomeScreen
import com.noor.islamic.presentation.onboarding.OnboardingScreen
import com.noor.islamic.presentation.prayer.PrayerScreen
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.presentation.qibla.QiblaScreen
import com.noor.islamic.presentation.quran.QuranReaderScreen
import com.noor.islamic.presentation.quran.QuranScreen
import com.noor.islamic.presentation.settings.SettingsScreen
import com.noor.islamic.presentation.tasbih.TasbihScreen

/**
 * Defines the navigation graph of the app.
 */
@Composable
fun NoorNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
    settings: AppSettings,
    prayerViewModel: PrayerViewModel,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onOpenDestination: (String) -> Unit = {},
    onOnboardingComplete: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(NoorDestinations.ONBOARDING) {
            OnboardingScreen(
                selectedLanguage = settings.language,
                onLanguageSelected = onLanguageSelected,
                onContinue = onOnboardingComplete,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.HOME) {
            HomeScreen(
                prayerViewModel = prayerViewModel,
                onOpenDestination = onOpenDestination,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.QURAN) {
            QuranScreen(
                onSurahClick = { surahNumber ->
                    navController.navigate(NoorDestinations.quranReaderRoute(surahNumber)) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(
            route = NoorDestinations.QURAN_READER,
            arguments = listOf(
                navArgument(NoorDestinations.QURAN_SURAH_ARG) {
                    type = NavType.IntType
                },
            ),
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt(
                NoorDestinations.QURAN_SURAH_ARG,
                -1,
            ) ?: -1
            QuranReaderScreen(
                surahNumber = surahNumber,
                onBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.PRAYER) {
            PrayerScreen(
                viewModel = prayerViewModel,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.ATHKAR) {
            AthkarScreen(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.QIBLA) {
            QiblaScreen(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.TASBIH) {
            TasbihScreen(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.SETTINGS) {
            SettingsScreen(
                language = settings.language,
                themeMode = settings.themeMode,
                onLanguageSelected = onLanguageSelected,
                onThemeSelected = onThemeSelected,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
    }
}