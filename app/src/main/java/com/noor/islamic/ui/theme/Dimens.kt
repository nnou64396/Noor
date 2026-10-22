package com.noor.islamic.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent spacing, radius and elevation scale for the Noor UI.
 *
 * The spacing scale is a 4dp-based ramp; the radius scale mirrors
 * [NoorShapes]; and the elevation scale is intentionally shallow, because
 * depth in this design comes from a hairline border plus a soft shadow rather
 * than from heavy drop shadows.
 */
object NoorDimens {
    // Spacing — 4dp ramp
    val Xs: Dp = 4.dp
    val Sm: Dp = 8.dp
    val Md: Dp = 16.dp
    val Lg: Dp = 24.dp
    val Xl: Dp = 32.dp
    val Xxl: Dp = 48.dp

    // Radii — mirrors NoorShapes
    val RadiusXs: Dp = 8.dp
    val RadiusSm: Dp = 12.dp
    val RadiusMd: Dp = 18.dp
    val RadiusLg: Dp = 26.dp
    val RadiusXl: Dp = 34.dp

    // Elevation
    val ElNone: Dp = 0.dp
    val ElSm: Dp = 2.dp
    val ElMd: Dp = 6.dp

    // Hairlines
    val Hairline: Dp = 1.dp

    // Component sizes
    val ButtonHeight: Dp = 52.dp
    val Icon: Dp = 24.dp
    val IconCircle: Dp = 56.dp
    val QuickCardHeight: Dp = 104.dp
    val BottomBarElevation: Dp = 12.dp
    val Logo: Dp = 44.dp
    val CornerLarge: Dp = 26.dp
    val QiblaDial: Dp = 260.dp

    // Minimum touch target, per the accessibility guidelines.
    val MinTouchTarget: Dp = 48.dp

    /**
     * Width cap for long-form reading content (empty-state copy, dialogs).
     *
     * Used as a `widthIn(max = ...)` rather than a fixed width, so short
     * strings stay narrow and the layout still behaves on small screens.
     */
    val ReadableWidthMax: Dp = 420.dp

    // Layout
    val BottomBarHeight: Dp = 68.dp
    val BottomBarIndicatorHeight: Dp = 30.dp
    val BottomBarIndicatorWidth: Dp = 58.dp
}
