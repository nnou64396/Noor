package dz.techvision.noor.navigation

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.domain.model.AppUpdateInfo
import dz.techvision.noor.domain.model.AsrSchool
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.Language
import dz.techvision.noor.domain.model.LocationMode
import dz.techvision.noor.domain.model.ThemeMode
import dz.techvision.noor.domain.model.UpdateStatus
import dz.techvision.noor.presentation.athkar.AthkarCategoryScreen
import dz.techvision.noor.presentation.athkar.AthkarCategoryViewModel
import dz.techvision.noor.presentation.athkar.AthkarScreen
import dz.techvision.noor.presentation.athkar.AthkarViewModel
import dz.techvision.noor.presentation.home.HomePracticeViewModel
import dz.techvision.noor.presentation.home.HomeQuranViewModel
import dz.techvision.noor.presentation.home.HomeScreen
import dz.techvision.noor.presentation.onboarding.OnboardingScreen
import dz.techvision.noor.presentation.prayer.PrayerScreen
import dz.techvision.noor.presentation.prayer.PrayerViewModel
import dz.techvision.noor.presentation.qibla.QiblaScreen
import dz.techvision.noor.presentation.quran.QuranBookmarksScreen
import dz.techvision.noor.presentation.quran.QuranReaderScreen
import dz.techvision.noor.presentation.quran.QuranScreen
import dz.techvision.noor.presentation.quran.QuranSearchScreen
import dz.techvision.noor.presentation.settings.SettingsScreen
import dz.techvision.noor.presentation.tasbih.TasbihScreen
import dz.techvision.noor.presentation.tasbih.TasbihViewModel
import dz.techvision.noor.services.ServiceLocator

/**
 * Defines the navigation graph of the app.
 *
 * The [NavGraphBuilder] builder is remembered for as long as [startDestination]
 * is unchanged. Navigation Compose keys the graph on that builder's identity,
 * so a stable builder avoids rebuilding the graph on every recomposition
 * (theme/language changes, content padding changes, etc.). Captured inputs are
 * read through [rememberUpdatedState] so the remembered builder always observes
 * the latest values without being recreated.
 */
@Composable
fun NoorNavHost(
    navController: NavHostController,
    startDestination: String,
    contentPadding: PaddingValues,
    settings: AppSettings,
    prayerViewModel: PrayerViewModel,
    updateStatus: UpdateStatus,
    onCheckForUpdates: () -> Unit,
    onDismissUpdate: (Long) -> Unit,
    onDownloadAndVerifyUpdate: (AppUpdateInfo) -> Unit,
    onInstallUpdate: (Context) -> Unit,
    onRetryUpdate: (AppUpdateInfo, Context) -> Unit,
    onOpenInstallSettings: () -> Unit,
    onLanguageSelected: (Language) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onCalculationMethodSelected: (CalculationMethod) -> Unit = {},
    onAsrSchoolSelected: (AsrSchool) -> Unit = {},
    onLocationModeSelected: (LocationMode) -> Unit = {},
    onManualLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    onOpenDestination: (String) -> Unit = {},
    onOnboardingComplete: () -> Unit,
) {
    val currentContentPadding by rememberUpdatedState(contentPadding)
    val currentSettings by rememberUpdatedState(settings)
    val currentPrayerViewModel by rememberUpdatedState(prayerViewModel)
    val currentUpdateStatus by rememberUpdatedState(updateStatus)
    val currentOnCheckForUpdates by rememberUpdatedState(onCheckForUpdates)
    val currentOnDismissUpdate by rememberUpdatedState(onDismissUpdate)
    val currentOnDownloadAndVerifyUpdate by rememberUpdatedState(onDownloadAndVerifyUpdate)
    val currentOnInstallUpdate by rememberUpdatedState(onInstallUpdate)
    val currentOnRetryUpdate by rememberUpdatedState(onRetryUpdate)
    val currentOnOpenInstallSettings by rememberUpdatedState(onOpenInstallSettings)
    val currentOnLanguageSelected by rememberUpdatedState(onLanguageSelected)
    val currentOnThemeSelected by rememberUpdatedState(onThemeSelected)
    val currentOnCalculationMethodSelected by rememberUpdatedState(onCalculationMethodSelected)
    val currentOnAsrSchoolSelected by rememberUpdatedState(onAsrSchoolSelected)
    val currentOnLocationModeSelected by rememberUpdatedState(onLocationModeSelected)
    val currentOnManualLocationSelected by rememberUpdatedState(onManualLocationSelected)
    val currentOnOpenDestination by rememberUpdatedState(onOpenDestination)
    val currentOnOnboardingComplete by rememberUpdatedState(onOnboardingComplete)

    val builder: NavGraphBuilder.() -> Unit = remember(navController, startDestination) {
        {
            composable(NoorDestinations.ONBOARDING) {
                OnboardingScreen(
                    selectedLanguage = currentSettings.language,
                    onLanguageSelected = currentOnLanguageSelected,
                    onContinue = currentOnOnboardingComplete,
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
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
                val practiceViewModel: HomePracticeViewModel = viewModel {
                    HomePracticeViewModel(
                        dhikrRepository = ServiceLocator.dhikrRepository,
                        progressRepository = ServiceLocator.athkarProgressRepository,
                        tasbihRepository = ServiceLocator.tasbihRepository,
                    )
                }
                val practiceState by practiceViewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    prayerViewModel = currentPrayerViewModel,
                    onOpenDestination = currentOnOpenDestination,
                    quran = quranState,
                    onTogglePlayback = homeQuranViewModel::togglePlayback,
                    practice = practiceState,
                    updateStatus = currentUpdateStatus,
                    onDismissUpdate = currentOnDismissUpdate,
                    onDownloadAndVerifyUpdate = currentOnDownloadAndVerifyUpdate,
                    onInstallUpdate = currentOnInstallUpdate,
                    onRetryUpdate = currentOnRetryUpdate,
                    onOpenInstallSettings = currentOnOpenInstallSettings,
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
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
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
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
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
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
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
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
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                )
            }
            composable(NoorDestinations.PRAYER) {
                PrayerScreen(
                    viewModel = currentPrayerViewModel,
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                )
            }
            composable(NoorDestinations.ATHKAR) {
                val athkarViewModel: AthkarViewModel = viewModel {
                    AthkarViewModel(
                        dhikrRepository = ServiceLocator.dhikrRepository,
                        progressRepository = ServiceLocator.athkarProgressRepository,
                    )
                }
                AthkarScreen(
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                    viewModel = athkarViewModel,
                    onOpenCategory = { categoryId ->
                        navController.navigate(NoorDestinations.athkarCategoryRoute(categoryId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = NoorDestinations.ATHKAR_CATEGORY,
                arguments = listOf(
                    navArgument(NoorDestinations.ATHKAR_CATEGORY_ARG) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments
                    ?.getString(NoorDestinations.ATHKAR_CATEGORY_ARG)
                    .orEmpty()
                val categoryViewModel: AthkarCategoryViewModel = viewModel {
                    AthkarCategoryViewModel(
                        categoryId = categoryId,
                        dhikrRepository = ServiceLocator.dhikrRepository,
                        progressRepository = ServiceLocator.athkarProgressRepository,
                    )
                }
                AthkarCategoryScreen(
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                    viewModel = categoryViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(NoorDestinations.TASBIH) {
                val tasbihViewModel: TasbihViewModel = viewModel {
                    TasbihViewModel(repository = ServiceLocator.tasbihRepository)
                }
                TasbihScreen(
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                    viewModel = tasbihViewModel,
                )
            }
            composable(NoorDestinations.QIBLA) {
                QiblaScreen(
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                )
            }
            composable(NoorDestinations.SETTINGS) {
                SettingsScreen(
                    language = currentSettings.language,
                    themeMode = currentSettings.themeMode,
                    calculationMethod = currentSettings.calculationMethod,
                    asrSchool = currentSettings.asrSchool,
                    locationMode = currentSettings.locationMode,
                    manualLatitude = currentSettings.manualLatitude,
                    manualLongitude = currentSettings.manualLongitude,
                    onLanguageSelected = currentOnLanguageSelected,
                    onThemeSelected = currentOnThemeSelected,
                    onCalculationMethodSelected = currentOnCalculationMethodSelected,
                    onAsrSchoolSelected = currentOnAsrSchoolSelected,
                    onLocationModeSelected = currentOnLocationModeSelected,
                    onManualLocationSelected = currentOnManualLocationSelected,
                    updateStatus = currentUpdateStatus,
                    onCheckForUpdates = currentOnCheckForUpdates,
                    onDismissUpdate = currentOnDismissUpdate,
                    onDownloadAndVerifyUpdate = currentOnDownloadAndVerifyUpdate,
                    onInstallUpdate = currentOnInstallUpdate,
                    onRetryUpdate = currentOnRetryUpdate,
                    onOpenInstallSettings = currentOnOpenInstallSettings,
                    modifier = Modifier.fillMaxSize().padding(currentContentPadding),
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        builder = builder,
    )
}
