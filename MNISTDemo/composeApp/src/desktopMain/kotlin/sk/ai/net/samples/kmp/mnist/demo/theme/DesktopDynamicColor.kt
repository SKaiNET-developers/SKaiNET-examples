package sk.ai.net.samples.kmp.mnist.demo.theme

import androidx.compose.runtime.Composable

/**
 * Provides desktop-specific dynamic color information.
 * Currently, dynamic colors are not supported on desktop.
 */
@Composable
actual fun platformDynamicColorInfo(): DynamicColorInfo {
    return DynamicColorInfo(
        isDynamicColorSupported = false,
        isDynamicColorEnabled = false
    )
}