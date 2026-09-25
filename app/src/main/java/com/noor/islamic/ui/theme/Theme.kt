package com.noor.islamic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.noor.islamic.domain.model.ThemeMode
import android.app.Activity

private val LightColors = lightColorScheme(
    primary = NoorEmerald,
    onPrimary = Color.White,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = OnLightEmeraldContainer,
    inversePrimary = NoorGold,
    secondary = NoorGold,
    onSecondary = OnLightGoldContainer,
    secondaryContainer = LightGoldContainer,
    onSecondaryContainer = OnLightGoldContainer,
    tertiary = LightTeal,
    background = NoorIvory,
    onBackground = NoorNavy,
    surface = NoorIvory,
    onSurface = NoorNavy,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,
    outline = ColorLightOutline,
    outlineVariant = LightOutlineVariant,
)

private val DarkColors = darkColorScheme(
    primary = DarkEmerald,
    onPrimary = OnDarkEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = OnDarkEmeraldContainer,
    inversePrimary = NoorGold,
    secondary = NoorGold,
    onSecondary = OnDarkGoldContainer,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = OnDarkGoldContainer,
    tertiary = DarkEmerald,
    background = NoorNavy,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = NoorNavy,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = ColorDarkOutline,
    outlineVariant = DarkOutlineVariant,
)

/**
 * Applies the Noor Material 3 theme. Dark mode is derived from the
 * persisted [themeMode] preference, not inverted from the light palette.
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

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NoorTypography,
        shapes = NoorShapes,
        content = content,
    )
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