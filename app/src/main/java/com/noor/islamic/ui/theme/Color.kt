package com.noor.islamic.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette --------------------------------------------------------------

/** Gold — عائدية عثمانية */
val NoorGold = Color(0xFFC9A227)
/** Deep Emerald — زمردي عميق */
val NoorEmerald = Color(0xFF0B3D2E)
/** Ivory — عاجي دافئ */
val NoorIvory = Color(0xFFFAF7F0)
/** Dark Navy — كحلي داكن */
val NoorNavy = Color(0xFF0A1628)

// Glass / surface layer colors ------------------------------------------------
// Translucent warm layers that give the "liquid glass" effect on top of the
// base background. Opacity is applied by the components so cards stay cheap
// to render (no backdrop blur needed).

/** Warm translucent "glass" white used on light backgrounds */
val NoorGlassLight = Color(0xFFFEFDF9)
/** Translucent navy "glass" used on dark backgrounds */
val NoorGlassDark = Color(0xFF16233A)
/** Translucent gold-tinted hairline border for glass surfaces */
val NoorGlassBorderLight = Color(0x4DC9A227)
val NoorGlassBorderDark = Color(0x4046616E)
/** Soft gold glow used behind primary actions and selection states */
val NoorGoldGlowLight = Color(0x33C9A227)
val NoorGoldGlowDark = Color(0x2EC9A227)

// Light scheme derived tones ---------------------------------------------------

val LightEmeraldContainer = Color(0xFFD8EBE0)
val OnLightEmeraldContainer = Color(0xFF05271C)
val LightGoldContainer = Color(0xFFF6E9C6)
val OnLightGoldContainer = Color(0xFF4A3A00)
val LightTeal = Color(0xFF2E7D6B)
val LightSurfaceVariant = Color(0xFFEDE9DE)
val OnLightSurfaceVariant = Color(0xFF4A5A51)
val ColorLightOutline = Color(0xFF7A847E)
val LightOutlineVariant = Color(0xFFCDC8BA)
val LightSurfaceContainerLow = Color(0xFFF4F0E6)
val LightSurfaceContainer = Color(0xFFEFEBE0)
val LightSurfaceContainerHigh = Color(0xFFE9E4D8)
val LightSurfaceContainerHighest = Color(0xFFE3DECF)
val LightSurfaceBright = Color(0xFFFFFFFF)

// Dark scheme derived tones ---------------------------------------------------

val DarkEmerald = Color(0xFF6FC7A0)
val OnDarkEmerald = Color(0xFF06281C)
val DarkEmeraldContainer = Color(0xFF0B3D2E)
val OnDarkEmeraldContainer = Color(0xFFB9E8CF)
val DarkGoldContainer = Color(0xFF5A4A00)
val OnDarkGoldContainer = Color(0xFFFFE9A8)
val DarkSurface = Color(0xFF101E30)
val OnDarkSurface = Color(0xFFE6EAF0)
val DarkSurfaceVariant = Color(0xFF1B2A3D)
val OnDarkSurfaceVariant = Color(0xFFB9C4CE)
val ColorDarkOutline = Color(0xFF8A95A0)
val DarkOutlineVariant = Color(0xFF46576F)
val DarkSurfaceContainerLow = Color(0xFF0F1D31)
val DarkSurfaceContainer = Color(0xFF14233A)
val DarkSurfaceContainerHigh = Color(0xFF193048)
val DarkSurfaceContainerHighest = Color(0xFF1E3550)
val DarkSurfaceBright = Color(0xFF33507A)