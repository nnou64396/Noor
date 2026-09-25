package com.noor.islamic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography foundation.
 *
 * Serif is used for display/headline roles to evoke the Ottoman feel and to
 * render Arabic glyphs gracefully; sans-serif is used for body and labels.
 *
 * Planned follow-up: swap [FontFamily.Serif] with a bundled/downloadable
 * Amiri build and [FontFamily.SansSerif] with Inter or Poppins.
 */
object NoorFonts {
    val Display: FontFamily = FontFamily.Serif
    val Body: FontFamily = FontFamily.SansSerif
}

private val baseLineHeights = Typography()

val NoorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 72.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 60.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = baseLineHeights.bodyMedium.copy(
        fontFamily = NoorFonts.Body,
        lineHeight = 24.sp,
    ),
    bodySmall = baseLineHeights.bodySmall.copy(
        fontFamily = NoorFonts.Body,
        lineHeight = 18.sp,
    ),
    labelLarge = baseLineHeights.labelLarge.copy(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = baseLineHeights.labelMedium.copy(
        fontFamily = NoorFonts.Body,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = baseLineHeights.labelSmall.copy(
        fontFamily = NoorFonts.Body,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

/**
 * Semantic text roles for screens. Screens use these aliases instead of
 * picking raw sizes, which keeps the tonal hierarchy consistent and lets
 * the scale be refined in one place.
 *
 * - [Typography.noorDisplay] — Display   (major Arabic/religious statements)
 * - [Typography.noorLargeTitle] — Large title (screen headers)
 * - [Typography.noorTitle] — Title       (card titles, key labels)
 * - [Typography.noorBody] — Body         (readable paragraphs)
 * - [Typography.noorLabel] — Label       (buttons, chips, compact emphasis)
 * - [Typography.noorCaption] — Caption   (subtle helper text)
 */
val Typography.noorDisplay: TextStyle get() = displayMedium
val Typography.noorLargeTitle: TextStyle get() = headlineMedium
val Typography.noorTitle: TextStyle get() = titleLarge
val Typography.noorBody: TextStyle get() = bodyLarge
val Typography.noorLabel: TextStyle get() = labelLarge
val Typography.noorCaption: TextStyle get() = bodySmall