package com.noor.islamic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography foundation — "Premium Blue".
 *
 * Serif carries the display and title roles; sans-serif carries body, labels
 * and numerals. Both map onto fonts the platform already ships, so Arabic,
 * English and French all render from one dependency-free strategy.
 *
 * Line heights are set generously on purpose. Arabic needs more leading than
 * Latin to stay comfortable, because ascenders, descenders and tashkeel marks
 * extend well beyond the em box. [NoorTypography] therefore favours air over
 * density, and the Quran reader gets its own, taller style via
 * [Typography.noorQuranArabic].
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
        fontSize = 44.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = baseLineHeights.bodyMedium.copy(
        fontFamily = NoorFonts.Body,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = baseLineHeights.bodySmall.copy(
        fontFamily = NoorFonts.Body,
        fontSize = 12.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = baseLineHeights.labelLarge.copy(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = baseLineHeights.labelMedium.copy(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = baseLineHeights.labelSmall.copy(
        fontFamily = NoorFonts.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

/**
 * Semantic text roles. Screens use these aliases rather than raw sizes, which
 * keeps the tonal hierarchy consistent and lets the scale be retuned in one
 * place.
 *
 * The scale reads, largest to smallest:
 *
 * - [Typography.noorDisplay]      — Display       hero numbers, prayer name
 * - [Typography.noorLargeTitle]   — Screen title  top of every screen
 * - [Typography.noorTitle]        — Section title section headings
 * - [Typography.noorCardTitle]    — Card title    card headline
 * - [Typography.noorBody]         — Body          readable paragraphs
 * - [Typography.noorLabel]        — Secondary     chips, buttons, emphasis
 * - [Typography.noorCaption]      — Caption       subtle helper text
 * - [Typography.noorQuranArabic]  — Quran text    the Arabic reader only
 */
val Typography.noorDisplay: TextStyle get() = displayMedium
val Typography.noorLargeTitle: TextStyle get() = headlineMedium
val Typography.noorTitle: TextStyle get() = titleLarge
val Typography.noorCardTitle: TextStyle get() = titleMedium
val Typography.noorBody: TextStyle get() = bodyLarge
val Typography.noorSecondary: TextStyle get() = bodyMedium
val Typography.noorLabel: TextStyle get() = labelLarge
val Typography.noorCaption: TextStyle get() = bodySmall

/**
 * The Arabic Quran text style.
 *
 * Deliberately not part of the shared scale: vocalised Arabic needs far more
 * vertical room than ordinary body copy, otherwise tashkeel marks collide with
 * the line above. Sized for sustained reading rather than for fitting a
 * screen.
 */
val Typography.noorQuranArabic: TextStyle
    get() = TextStyle(
        fontFamily = NoorFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    )
