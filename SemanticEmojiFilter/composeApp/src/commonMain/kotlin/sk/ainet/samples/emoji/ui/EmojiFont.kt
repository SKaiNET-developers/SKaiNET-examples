package sk.ainet.samples.emoji.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/**
 * The font the emoji glyphs are drawn with. Desktop and Android keep the system default, whose
 * fallback chain has a color emoji font; the web build provides the bundled Noto Color Emoji subset
 * (see scripts/subset-emoji-font.sh), because Skiko in a browser has no system fonts and would draw
 * every emoji as a replacement box.
 */
val LocalEmojiFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
