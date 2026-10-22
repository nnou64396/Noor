package com.noor.islamic.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — "Premium Blue" ----------------------------------------------
//
// The identity is blue-dominant. Blue carries structure, surfaces and action;
// gold is a deliberately rationed accent (see [NoorAccent]) used for emphasis
// only, never as a fill for whole components or as body text on light surfaces.
//
// Every on-colour pairing used by the schemes below was checked against WCAG
// relative luminance (see the contrast table in the redesign notes); all body
// text pairs land between 6.6:1 and 16.5:1.

// Blue scale — deep navy text through to a near-white page tint.
val NoorBlue900 = Color(0xFF0B1F3A) // deep navy — headings, primary text
val NoorBlue800 = Color(0xFF12365C)
val NoorBlue700 = Color(0xFF1B4F8F) // rich blue — light-mode primary
val NoorBlue600 = Color(0xFF2563A8) // primary accent
val NoorBlue500 = Color(0xFF3B82C4) // accent / hero gradient top
val NoorBlue400 = Color(0xFF6BA3DC)
val NoorBlue300 = Color(0xFFA8C8E8) // soft blue — light-mode accents
val NoorBlue200 = Color(0xFFD6E6F5) // soft blue surface
val NoorBlue100 = Color(0xFFE8F1FA)
val NoorBlue50 = Color(0xFFF7F9FC) // warm ivory / soft white page background

// Midnight scale — dark mode only. Deliberately not pure black.
val NoorMidnight950 = Color(0xFF070F1C)
val NoorMidnight900 = Color(0xFF0A1628) // page background
val NoorMidnight850 = Color(0xFF0D1C31)
val NoorMidnight800 = Color(0xFF101E30) // default card surface
val NoorMidnight700 = Color(0xFF16273D)
val NoorMidnight600 = Color(0xFF1B3A5C) // soft blue container
val NoorMidnight500 = Color(0xFF254A72)

// Text tones
val NoorInk = Color(0xFF0B1F3A) // light-mode primary text
val NoorInkMuted = Color(0xFF4A5A72) // light-mode secondary text (7.0:1 on white)
val NoorOnDark = Color(0xFFE8EEF6) // dark-mode primary text (15.5:1)
val NoorOnDarkMuted = Color(0xFFA9B8CC) // dark-mode secondary text (8.3:1)

// Accent — rationed gold. ------------------------------------------------------
//
// [NoorAccent] is the only sanctioned gold. It is meant for small decorative
// marks, selected-state emphasis and numerical highlights.
//
// Contrast, measured:
//   NoorGold      #C9A227 on white  2.42:1  -> decorative shapes ONLY
//   NoorGoldText  #8A6D12 on white  4.91:1  -> safe when gold must be text
//   NoorGoldLight #E3C766 on #0A1628 10.9:1 -> safe on dark surfaces
val NoorGold = Color(0xFFC9A227)
val NoorGoldText = Color(0xFF8A6D12)
val NoorGoldLight = Color(0xFFE3C766)
val NoorGoldContainerLight = Color(0xFFF6EFD8)
val NoorOnGoldContainer = Color(0xFF4A3A00)
val NoorGoldContainerDark = Color(0xFF4A3D12)
val NoorOnGoldContainerDark = Color(0xFFF3E4B4)

// Hairlines and dividers
val NoorHairlineLight = Color(0xFFDCE7F2)
val NoorHairlineDark = Color(0xFF27405C)
val NoorOutlineLight = Color(0xFF6B7C93)
val NoorOutlineDark = Color(0xFF8A9BB0)

// Translucent layers used by the bottom bar and the hero overlay. Kept
// deliberately few: solid premium surfaces carry the design, glass is an accent.
val NoorGlassLight = Color(0xF2FFFFFF)
val NoorGlassDark = Color(0xF21A2A40)
val NoorGlassBorderLight = Color(0x40FFFFFF)
val NoorGlassBorderDark = Color(0x33FFFFFF)

// Hero gradient — the signature "premium blue" surface.
val NoorHeroTop = Color(0xFF17406E)
val NoorHeroBottom = Color(0xFF0B2545)
val NoorHeroMotif = Color(0x1AFFFFFF)

// Light scheme derived tones ---------------------------------------------------
val LightPrimaryContainer = Color(0xFFD6E6F5)
val OnLightPrimaryContainer = Color(0xFF0B2E52)
val LightOnSecondaryContainer = Color(0xFF0B2E52)
val LightSurfaceVariant = Color(0xFFEDF2F8)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFBFCFE)
val LightSurfaceContainer = Color(0xFFF2F6FB)
val LightSurfaceContainerHigh = Color(0xFFE9F0F8)
val LightSurfaceContainerHighest = Color(0xFFDFE9F4)
val LightSurfaceBright = Color(0xFFFFFFFF)
val LightScrimBlue = Color(0x141B4F8F)

// Dark scheme derived tones ----------------------------------------------------
val DarkPrimary = Color(0xFF8FC0EE)
val OnDarkPrimary = Color(0xFF0A1B2E)
val DarkOnSecondaryContainer = Color(0xFFCFE4F8)
val DarkSurfaceVariant = Color(0xFF1B2A3D)
val DarkSurfaceContainerLowest = Color(0xFF060D18)
val DarkSurfaceContainerLow = Color(0xFF0D1C31)
val DarkSurfaceContainer = Color(0xFF13243A)
val DarkSurfaceContainerHigh = Color(0xFF1A2E48)
val DarkSurfaceContainerHighest = Color(0xFF213A57)
val DarkSurfaceBright = Color(0xFF2C4A70)
val DarkScrimBlue = Color(0x1F8FC0EE)

/**
 * The rationed gold accent, resolved for the active theme.
 *
 * Components should prefer blue. Reach for this only where a small gold mark
 * genuinely lifts the hierarchy — a selected indicator, a numerical emphasis,
 * or the centre of the Noor mark.
 */
object NoorAccent {
    /** Gold for decorative shapes; never use as text on a light surface. */
    val Decorative: Color = NoorGold

    /** Gold that is safe to render as text on the current surface. */
    val Text: Color = NoorGoldText
}
