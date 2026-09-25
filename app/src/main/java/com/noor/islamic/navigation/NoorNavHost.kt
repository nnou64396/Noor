package com.noor.islamic.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.noor.islamic.domain.model.AppSettings
import com.noor.islamic.domain.model.Language
import com.noor.islamic.domain.model.ThemeMode
import com.noor.islamic.presentation.athkar.AthkarScreen
import com.noor.islamic.presentation.home.HomeQuranViewModel
import com.noor.islamic.presentation.home.HomeScreen
import com.noor.islamic.presentation.onboarding.OnboardingScreen
import com.noor.islamic.presentation.prayer.PrayerScreen
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.presentation.qibla.QiblaScreen
import com.noor.islamic.presentation.quran.QuranBookmarksScreen
import com.noor.islamic.presentation.quran.QuranReaderScreen
import com.noor.islamic.presentation.quran.QuranScreen
import com.noor.islamic.presentation.quran.QuranSearchScreen
import com.noor.islamic.presentation.settings.SettingsScreen
import com.noor.islamic.presentation.tasbih.TasbihScreen
import com.noor.islamic.services.ServiceLocator

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
            val homeQuranViewModel: HomeQuranViewModel = viewModel {
                HomeQuranViewModel(
                    readingProgress = ServiceLocator.readingProgressRepository,
                    repository = ServiceLocator.quranRepository,
                    audio = ServiceLocator.quranAudioController,
                )
            }
            val quranState by homeQuranViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                prayerViewModel = prayerViewModel,
                onOpenDestination = onOpenDestination,
                quran = quranState,
                onTogglePlayback = homeQuranViewModel::togglePlayback,
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
                onSearchClick = { navController.navigate(NoorDestinations.QURAN_SEARCH) },
                onBookmarksClick = { navController.navigate(NoorDestinations.QURAN_BOOKMARKS) },
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.QURAN_SEARCH) {
            QuranSearchScreen(
                onBack = { navController.popBackStack() },
                onOpenAyah = { surahNumber, ayahNumber ->
                    navController.navigate(
                        NoorDestinations.quranAyahRoute(surahNumber, ayahNumber),
                    )
                },
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }
        composable(NoorDestinations.QURAN_BOOKMARKS) {
            QuranBookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpenAyah = { surahNumber, ayahNumber ->
                    navController.navigate(
                        NoorDestinations.quranAyahRoute(surahNumber, ayahNumber),
                    )
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
                navArgument(NoorDestinations.QURAN_AYAH_ARG) {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt(
                NoorDestinations.QURAN_SURAH_ARG,
                -1,
            ) ?: -1
            val requestedAyah = backStackEntry.arguments?.getInt(
                NoorDestinations.QURAN_AYAH_ARG,
                -1,
            ) ?: -1
            QuranReaderScreen(
                surahNumber = surahNumber,
                onBack = { navController.popBackStack() },
                initialAyahNumber = requestedAyah.takeIf { it > 0 },
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