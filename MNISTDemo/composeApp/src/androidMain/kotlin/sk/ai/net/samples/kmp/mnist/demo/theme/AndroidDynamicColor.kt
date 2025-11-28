package sk.ai.net.samples.kmp.mnist.demo.theme

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Provides Android-specific dynamic color information.
 * On Android 12+ (API level 31+), dynamic colors are supported through Material You.
 */
@Composable
actual fun platformDynamicColorInfo(): DynamicColorInfo {
    val isAndroid12OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return DynamicColorInfo(
        isDynamicColorSupported = isAndroid12OrHigher,
        isDynamicColorEnabled = isAndroid12OrHigher
    )
}

/**
 * Gets the appropriate color scheme for Android, using dynamic colors if available.
 */
@Composable
fun getAndroidColorScheme(darkTheme: Boolean, dynamicColorInfo: DynamicColorInfo) = when {
    dynamicColorInfo.isDynamicColorSupported && dynamicColorInfo.isDynamicColorEnabled -> {
        val context = LocalContext.current
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
