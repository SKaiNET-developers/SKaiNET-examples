package sk.ainet.samples.visiondemo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The SKaiNET design system of the lovable prototype, converted from its oklch tokens — in a dark
 * and a light rendition. The glass HUD that floats over camera frames keeps its dark scrim in both
 * themes (text over video needs it), which is what [onGlass] is for.
 */
@Immutable
data class Palette(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val surface: Color,
    val primary: Color,
    val primaryGlow: Color,
    val secondaryFg: Color,
    val mutedFg: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val scrim: Color,
    val glassBorder: Color,
    val onGlass: Color,
) {
    /** Box palette: primary red, cyan-blue, green, warning amber, pink. */
    val boxes: List<Color> get() = listOf(primary, Color(0xFF00B5F5), success, warning, Color(0xFFE068D8))
    fun boxColor(i: Int) = boxes[((i % boxes.size) + boxes.size) % boxes.size]
}

val DarkPalette = Palette(
    background = Color(0xFF06070A),
    foreground = Color(0xFFF2F2F2),
    card = Color(0xFF101215),
    surface = Color(0xFF0A0C0F),
    primary = Color(0xFFDC2626),
    primaryGlow = Color(0xFFEF4444),
    secondaryFg = Color(0xFFD7D7D7),
    mutedFg = Color(0xFF6B727E),
    border = Color(0xFF25292F),
    success = Color(0xFF4BCB71),
    warning = Color(0xFFF0B135),
    scrim = Color(0xFF030304).copy(alpha = 0.62f),
    glassBorder = Color(0xFFF2F2F2).copy(alpha = 0.10f),
    onGlass = Color(0xFFF2F2F2),
)

val LightPalette = Palette(
    background = Color(0xFFF6F6F8),
    foreground = Color(0xFF0B0C0F),
    card = Color(0xFFFFFFFF),
    surface = Color(0xFFECEDF1),
    primary = Color(0xFFDC2626),
    primaryGlow = Color(0xFFEF4444),
    secondaryFg = Color(0xFF2A2C31),
    mutedFg = Color(0xFF6B727E),
    border = Color(0xFFD9DBE1),
    success = Color(0xFF1F9D4D),
    warning = Color(0xFFB07B10),
    scrim = Color(0xFF030304).copy(alpha = 0.62f),   // glass over camera stays dark
    glassBorder = Color(0xFFF2F2F2).copy(alpha = 0.14f),
    onGlass = Color(0xFFF2F2F2),
)

val LocalDt = staticCompositionLocalOf { DarkPalette }
