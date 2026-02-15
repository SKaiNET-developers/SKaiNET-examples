package sk.ainet.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the colors used by the loading indicator component.
 * This data class provides theme-aware color properties for all visual elements
 * of the IndeterminateOrbitingFadingRingLoader.
 */
@Immutable
data class LoadingIndicatorColors(
    val ringColor: Color,
    val nodeColor: Color,
    val edgeColor: Color,
    val rightNodeColor: Color
)

/**
 * Contains default values and factory functions for LoadingIndicator components.
 * Provides Material Theme integration and appropriate defaults for different use cases.
 */
object LoadingIndicatorDefaults {
    
    /**
     * Creates a LoadingIndicatorColors instance with Material Theme integration.
     * 
     * @param ringColor Color for the main fading ring. Defaults to primary color.
     * @param nodeColor Color for the left triangle nodes. Defaults to onSurfaceVariant.
     * @param edgeColor Color for the triangle edges. Defaults to onSurfaceVariant.
     * @param rightNodeColor Color for the right triangle node. Defaults to primary color.
     * @return LoadingIndicatorColors configured with the specified or default colors.
     */
    @Composable
    fun colors(
        ringColor: Color = MaterialTheme.colorScheme.primary,
        nodeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        edgeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        rightNodeColor: Color = MaterialTheme.colorScheme.primary
    ): LoadingIndicatorColors = LoadingIndicatorColors(
        ringColor = ringColor,
        nodeColor = nodeColor,
        edgeColor = edgeColor,
        rightNodeColor = rightNodeColor
    )
    
    /**
     * Creates LoadingIndicatorColors specifically optimized for search field usage.
     * Uses more subtle colors that work well in compact UI contexts.
     * 
     * @return LoadingIndicatorColors configured for search field usage.
     */
    @Composable
    fun searchFieldColors(): LoadingIndicatorColors = LoadingIndicatorColors(
        ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        nodeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        edgeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        rightNodeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    )
}

/**
 * A simplified wrapper around IndeterminateOrbitingFadingRingLoader that provides
 * appropriate defaults for common use cases.
 * 
 * This composable follows PascalCase naming convention and provides a clean API
 * for integrating the custom loading indicator throughout the application.
 * 
 * @param modifier Modifier to be applied to the loading indicator.
 * @param size Size of the loading indicator. Defaults to 18dp for search field compatibility.
 * @param colors Colors to use for the loading indicator. Defaults to theme-aware colors.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    colors: LoadingIndicatorColors = LoadingIndicatorDefaults.colors()
) {
    indeterminateOrbitingFadingRingLoader(
        modifier = modifier,
        size = size,
        rotationDurationMillis = 1200,
        ringCycleDurationMillis = 1400,
        ringColor = colors.ringColor,
        nodeColor = colors.nodeColor,
        edgeColor = colors.edgeColor,
        rightNodeColor = colors.rightNodeColor,
        minRingAlpha = 0.15f,
        maxRingAlpha = 1f,
        triangleAngleOffset = 0f
    )
}