package dz.techvision.noor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dz.techvision.noor.domain.model.ThemeMode

/**
 * Whether the *resolved* Noor theme is dark.
 *
 * Components must read this rather than `isSystemInDarkTheme()`: the user can
 * pin Light or Dark independently of the system setting, and the scheme below
 * is driven by the persisted preference. Reading the system flag directly made
 * component chrome disagree with the scheme whenever the two differed.
 */
val LocalNoorIsDark = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = NoorBlue700,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = OnLightPrimaryContainer,
    inversePrimary = NoorBlue300,
    // Blue, not gold: `secondary` is the accent role used by dividers, section
    // icons, quick-card chips and the navigation indicator, so this single
    // assignment is what keeps the product reading as blue.
    secondary = NoorBlue600,
    onSecondary = Color.White,
    secondaryContainer = LightPrimaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    // Gold is rationed to `tertiary`; it is used only where a small mark should
    // lift the hierarchy. A text-safe gold is used here so that even an
    // incidental use never falls below the contrast floor.
    tertiary = NoorGoldText,
    onTertiary = Color.White,
    tertiaryContainer = NoorGoldContainerLight,
    onTertiaryContainer = NoorOnGoldContainer,
    background = NoorBlue50,
    onBackground = NoorInk,
    surface = NoorBlue50,
    onSurface = NoorInk,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = NoorInkMuted,
    outline = NoorOutlineLight,
    outlineVariant = NoorHairlineLight,
    scrim = LightScrimBlue,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = NoorMidnight600,
    onPrimaryContainer = DarkOnSecondaryContainer,
    inversePrimary = NoorBlue700,
    secondary = NoorBlue300,
    onSecondary = Color(0xFF06203A),
    secondaryContainer = NoorMidnight600,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = NoorGoldLight,
    onTertiary = Color(0xFF3A2E08),
    tertiaryContainer = NoorGoldContainerDark,
    onTertiaryContainer = NoorOnGoldContainerDark,
    background = NoorMidnight900,
    onBackground = NoorOnDark,
    surface = NoorMidnight900,
    onSurface = NoorOnDark,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = NoorOnDarkMuted,
    outline = NoorOutlineDark,
    outlineVariant = NoorHairlineDark,
    scrim = DarkScrimBlue,
)

/**
 * Applies the Noor Material 3 theme. Dark mode is derived from the persisted
 * [themeMode] preference, never by inverting the light palette.
 */
@Composable
fun NoorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    NoorSystemBars(darkTheme)

    CompositionLocalProvider(LocalNoorIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = NoorTypography,
            shapes = NoorShapes,
            content = content,
        )
    }
}

/**
 * Keeps the status/navigation bar icons readable on transparent bars.
 */
@Composable
private fun NoorSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

