package sk.ai.net.samples.kmp.mnist.demo.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Window size classes for responsive UI
 */
enum class WindowSizeClass {
    COMPACT,  // Phone in portrait
    MEDIUM,   // Phone in landscape, tablet in portrait
    EXPANDED  // Tablet in landscape
}

/**
 * Screen orientation
 */
enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * A composable that provides responsive layout based on screen size and orientation
 */
@Composable
fun ResponsiveLayout(
    content: @Composable (WindowSizeClass, Orientation) -> Unit
) {
    BoxWithConstraints {
        // Define breakpoints
        val compactWidth = 600.dp
        val mediumWidth = 840.dp

        // Get dimensions from constraints
        val width = maxWidth
        val height = maxHeight

        // Determine orientation
        val orientation = if (width < height) Orientation.PORTRAIT else Orientation.LANDSCAPE

        // Determine window size class based on width
        val windowSizeClass = when {
            width < compactWidth -> WindowSizeClass.COMPACT
            width < mediumWidth -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        // Call the content with the determined size class and orientation
        content(windowSizeClass, orientation)
    }
}

/**
 * Extension function to check if the window size class is compact
 */
fun WindowSizeClass.isCompact() = this == WindowSizeClass.COMPACT

/**
 * Extension function to check if the window size class is medium
 */
fun WindowSizeClass.isMedium() = this == WindowSizeClass.MEDIUM

/**
 * Extension function to check if the window size class is expanded
 */
fun WindowSizeClass.isExpanded() = this == WindowSizeClass.EXPANDED

/**
 * Extension function to check if the orientation is portrait
 */
fun Orientation.isPortrait() = this == Orientation.PORTRAIT

/**
 * Extension function to check if the orientation is landscape
 */
fun Orientation.isLandscape() = this == Orientation.LANDSCAPE
