package sk.ainet.ui.plot

import androidx.compose.ui.graphics.Color

/**
 * Configuration for plot padding (space for axes and labels).
 */
data class PlotPadding(
    val left: Float = 50f,
    val right: Float = 10f,
    val top: Float = 10f,
    val bottom: Float = 30f
)

/**
 * Configuration for axis display.
 */
data class AxisConfig(
    val show: Boolean = true,
    val showTicks: Boolean = true,
    val showLabels: Boolean = true,
    val tickCount: Int = 3,
    val labelFormatter: (Float) -> String = { it.toString() },
    val color: Color = Color.Gray,
    val strokeWidth: Float = 1.5f,
    val tickLength: Float = 5f
)

/**
 * Configuration for grid lines.
 */
data class GridConfig(
    val showHorizontal: Boolean = true,
    val showVertical: Boolean = true,
    val color: Color = Color.Gray.copy(alpha = 0.2f),
    val strokeWidth: Float = 1f
)
