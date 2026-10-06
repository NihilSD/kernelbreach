package com.kernelbreach.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Corner radii, tap targets and spacing from the design. */
@Immutable
data class KbShapes(
    val card: androidx.compose.ui.unit.Dp = 20.dp,
    val cardSmall: androidx.compose.ui.unit.Dp = 18.dp,
    val button: androidx.compose.ui.unit.Dp = 16.dp,
    val chip: androidx.compose.ui.unit.Dp = 999.dp,
    val minTarget: androidx.compose.ui.unit.Dp = 44.dp,
    val primaryButtonHeight: androidx.compose.ui.unit.Dp = 56.dp,
    val screenPadding: androidx.compose.ui.unit.Dp = 20.dp,
)

val LocalKbShapes = staticCompositionLocalOf { KbShapes() }

private fun materialLight() = lightColorScheme(
    primary = LightKbColors.ink,
    onPrimary = LightKbColors.onInk,
    background = LightKbColors.paper,
    onBackground = LightKbColors.ink,
    surface = LightKbColors.card,
    onSurface = LightKbColors.ink,
    error = LightKbColors.danger,
    secondary = LightKbColors.link,
)

private fun materialDark() = darkColorScheme(
    primary = DarkKbColors.ink,
    onPrimary = DarkKbColors.onInk,
    background = DarkKbColors.paper,
    onBackground = DarkKbColors.ink,
    surface = DarkKbColors.card,
    onSurface = DarkKbColors.ink,
    error = DarkKbColors.danger,
    secondary = DarkKbColors.link,
)

/**
 * The app theme. Wraps Material 3 and exposes the Blueprint tokens through
 * [KbTheme] (colours, type, shapes). [forceDark] lets the You screen override the
 * system setting.
 */
@Composable
fun KernelBreachTheme(
    forceDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = forceDark ?: isSystemInDarkTheme()
    val kbColors = if (dark) DarkKbColors else LightKbColors
    CompositionLocalProvider(
        LocalKbColors provides kbColors,
        LocalKbTypography provides KbType,
        LocalKbShapes provides KbShapes(),
    ) {
        MaterialTheme(
            colorScheme = if (dark) materialDark() else materialLight(),
            typography = KbMaterialTypography,
            content = content,
        )
    }
}

/** Convenient accessors: `KbTheme.colors`, `KbTheme.type`, `KbTheme.shapes`. */
object KbTheme {
    val colors: KbColors
        @Composable get() = LocalKbColors.current
    val type: KbTypography
        @Composable get() = LocalKbTypography.current
    val shapes: KbShapes
        @Composable get() = LocalKbShapes.current
}
