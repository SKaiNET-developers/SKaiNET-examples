package sk.ainet.samples.kmp.sinus.approximator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Theme controller for manual dark/light mode switching.
 * Used on WASM platform where system theme detection may not work reliably.
 */
class ThemeController {
    var isDarkTheme by mutableStateOf(true) // Default to dark theme

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
}

/**
 * CompositionLocal to access the theme controller from anywhere in the UI tree.
 */
val LocalThemeController = compositionLocalOf<ThemeController?> { null }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    primaryContainer = DarkPrimaryGlow,
    onPrimaryContainer = DarkPrimaryForeground,
    
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryForeground,
    secondaryContainer = DarkSecondary,
    onSecondaryContainer = DarkSecondaryForeground,
    
    tertiary = DarkAccent,
    onTertiary = DarkAccentForeground,
    tertiaryContainer = DarkAccent,
    onTertiaryContainer = DarkAccentForeground,
    
    background = DarkBackground,
    onBackground = DarkForeground,
    
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedForeground,
    
    error = DarkDestructive,
    onError = DarkDestructiveForeground,
    errorContainer = DarkDestructive,
    onErrorContainer = DarkDestructiveForeground,
    
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    
    inverseSurface = LightSurface,
    inverseOnSurface = LightForeground,
    inversePrimary = LightPrimary,
    
    surfaceTint = DarkPrimary,
    scrim = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    primaryContainer = LightPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = LightPrimary,
    
    secondary = LightSecondary,
    onSecondary = LightSecondaryForeground,
    secondaryContainer = LightSecondary,
    onSecondaryContainer = LightSecondaryForeground,
    
    tertiary = LightAccent,
    onTertiary = LightAccentForeground,
    tertiaryContainer = LightAccent.copy(alpha = 0.1f),
    onTertiaryContainer = LightAccent,
    
    background = LightBackground,
    onBackground = LightForeground,
    
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedForeground,
    
    error = LightDestructive,
    onError = LightDestructiveForeground,
    errorContainer = LightDestructive.copy(alpha = 0.1f),
    onErrorContainer = LightDestructive,
    
    outline = LightBorder,
    outlineVariant = LightBorder,
    
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkForeground,
    inversePrimary = DarkPrimary,
    
    surfaceTint = LightPrimary,
    scrim = LightBackground
)

@Composable
fun SKaiNETTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeController: ThemeController? = null,
    content: @Composable () -> Unit
) {
    // Use themeController if provided (WASM), otherwise use system theme
    val isDark = themeController?.isDarkTheme ?: darkTheme
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeController provides themeController) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SKaiNETTypography,
            content = content
        )
    }
}
