package com.kernelbreach.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography for the Blueprint design.
 *
 * The design calls for three bundled families — Bricolage Grotesque (headlines),
 * Hanken Grotesk (body) and IBM Plex Mono (code). Font binaries are NOT in the
 * content bundle, so to keep the project compiling the families fall back to
 * system equivalents. Dropping the `.ttf` files into `res/font` and swapping the
 * three `FontFamily` values below is all that's needed to match the design and
 * stay fully offline. See README "Known gaps".
 */
val DisplayFamily: FontFamily = FontFamily.SansSerif // -> Bricolage Grotesque
val BodyFamily: FontFamily = FontFamily.SansSerif // -> Hanken Grotesk
val MonoFamily: FontFamily = FontFamily.Monospace // -> IBM Plex Mono

/** Extra, design-specific text styles not covered by Material's [Typography]. */
@Immutable
data class KbTypography(
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val overline: TextStyle,
    val mono: TextStyle,
)

val KbType = KbTypography(
    headline = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    title = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    body = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    label = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    overline = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
    mono = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
)

/** Material 3 typography mapped onto the Blueprint styles. */
val KbMaterialTypography = Typography(
    headlineLarge = KbType.headline,
    headlineMedium = KbType.title,
    titleLarge = KbType.title,
    bodyLarge = KbType.body,
    bodyMedium = KbType.body,
    labelLarge = KbType.label,
    labelSmall = KbType.overline,
)

val LocalKbTypography = staticCompositionLocalOf { KbType }
