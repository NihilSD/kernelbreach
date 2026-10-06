package com.kernelbreach.core.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The "Blueprint" colour tokens from the design spec. These are the source of
 * truth for colour; Material 3's [androidx.compose.material3.ColorScheme] is
 * derived from them in [KernelBreachTheme] only so stock M3 components look right.
 */
@Immutable
data class KbColors(
    val ink: Color,
    val paper: Color,
    val card: Color,
    val dotGrid: Color,
    val highlighter: Color,
    val softYellow: Color,
    val link: Color,
    val mutedStrong: Color,
    val muted: Color,
    val border: Color,
    val success: Color,
    val successTint: Color,
    val warning: Color,
    val danger: Color,
    val onInk: Color,
    val isDark: Boolean,
)

val LightKbColors = KbColors(
    ink = Color(0xFF101B33),
    paper = Color(0xFFF3F5F9),
    card = Color(0xFFFFFFFF),
    dotGrid = Color(0xFFD3DAE8),
    highlighter = Color(0xFFFFE066),
    softYellow = Color(0xFFFFF1AD),
    link = Color(0xFF2347C7),
    mutedStrong = Color(0xFF46546F),
    muted = Color(0xFF5B6781),
    border = Color(0xFFDCE2EC),
    success = Color(0xFF0F9D8A),
    successTint = Color(0xFFDDF3EF),
    warning = Color(0xFFE0A100),
    danger = Color(0xFFE5532D),
    onInk = Color(0xFFFFFFFF),
    isDark = false,
)

// Dark tokens derived per spec: paper #0E1424, card #161E33, ink text #EAF0FF,
// highlighter kept yellow. Supporting colours nudged for contrast on dark paper.
val DarkKbColors = KbColors(
    ink = Color(0xFFEAF0FF),
    paper = Color(0xFF0E1424),
    card = Color(0xFF161E33),
    dotGrid = Color(0xFF263150),
    highlighter = Color(0xFFFFE066),
    softYellow = Color(0xFF4A431F),
    link = Color(0xFF8FA6FF),
    mutedStrong = Color(0xFFAEBAD4),
    muted = Color(0xFF8C98B4),
    border = Color(0xFF2A3350),
    success = Color(0xFF3FB8A6),
    successTint = Color(0xFF13342F),
    warning = Color(0xFFF0BE4A),
    danger = Color(0xFFF07A57),
    onInk = Color(0xFF0E1424),
    isDark = true,
)

/** Brand colours per path. Prefer the curriculum's own `color` when available. */
object PathPalette {
    val rookie = Color(0xFF0F9D8A)
    val core = Color(0xFF2F6BFF)
    val soc = Color(0xFFC2358A)
    val pen = Color(0xFFE5532D)
    val eng = Color(0xFFE0A100)
    val ai = Color(0xFF7B4DFF)

    fun forKey(key: String): Color = when (key.uppercase()) {
        "ROOK" -> rookie
        "CORE" -> core
        "SOC" -> soc
        "PEN" -> pen
        "ENG" -> eng
        "AI" -> ai
        else -> core
    }

    /** Parse a `#RRGGBB` hex string, falling back to the keyed palette colour. */
    fun parse(hex: String?, key: String): Color {
        if (hex != null && hex.startsWith("#") && (hex.length == 7 || hex.length == 9)) {
            runCatching {
                val clean = hex.removePrefix("#")
                val value = clean.toLong(16)
                return if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
            }
        }
        return forKey(key)
    }
}

val LocalKbColors = staticCompositionLocalOf { LightKbColors }
