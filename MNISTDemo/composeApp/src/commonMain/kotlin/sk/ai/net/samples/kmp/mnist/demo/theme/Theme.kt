package sk.ai.net.samples.kmp.mnist.demo.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Platform-specific implementation for dynamic color support.
 * Each platform will provide its own implementation.
 */
@Composable
expect fun platformDynamicColorInfo(): DynamicColorInfo

/**
 * Information about the current platform's dynamic color support.
 * This will be overridden by platform-specific implementations.
 */
data class DynamicColorInfo(
    val isDynamicColorSupported: Boolean = false,
    val isDynamicColorEnabled: Boolean = false
)

/**
 * CompositionLocal to provide dynamic color information across the app
 */
val LocalDynamicColorInfo = staticCompositionLocalOf { DynamicColorInfo() }

/**
 * Extension property to easily access dynamic color info from MaterialTheme
 */
val MaterialTheme.dynamicColorInfo: DynamicColorInfo
    @Composable
    @ReadOnlyComposable
    get() = LocalDynamicColorInfo.current

/**
 * App theme that applies Material Design 3 styling to the entire application.
 * 
 * @param darkTheme Whether to use dark theme colors
 * @param dynamicColorInfo Information about dynamic color support on the current platform
 * @param content The content to be styled
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Get platform-specific dynamic color information
    dynamicColorInfo: DynamicColorInfo = platformDynamicColorInfo(),
    content: @Composable () -> Unit
) {
    // Use the appropriate color scheme based on the theme mode
    val colorScheme = when {
        // Use dynamic colors if supported and enabled
        dynamicColorInfo.isDynamicColorSupported && dynamicColorInfo.isDynamicColorEnabled -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
            // Note: On Android, we would use dynamicDarkColorScheme() or dynamicLightColorScheme()
            // but we're using our predefined schemes for cross-platform compatibility
        }
        // Otherwise use our predefined color schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Provide the dynamic color info through CompositionLocal
    CompositionLocalProvider(
        LocalDynamicColorInfo provides dynamicColorInfo
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * A convenience wrapper around Surface that applies the theme's background color.
 * Use this for screens and other large containers.
 */
@Composable
fun AppSurface(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        content = content
    )
}
