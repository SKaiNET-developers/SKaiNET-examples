package sk.ainet.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * Scope for drawing within a plot. Provides coordinate transformation methods
 * and access to the drawing scope and text measurer.
 */
class PlotScope(
    val bounds: PlotBounds,
    val plotArea: Rect,
    val drawScope: DrawScope,
    val textMeasurer: TextMeasurer,
    val textStyle: TextStyle
) {
    /**
     * Convert data X coordinate to pixel X coordinate.
     */
    fun dataToPixelX(x: Float): Float {
        return plotArea.left + ((x - bounds.xMin) / bounds.xRange) * plotArea.width
    }

    /**
     * Convert data Y coordinate to pixel Y coordinate.
     * Note: Y is inverted (higher values at top of screen).
     */
    fun dataToPixelY(y: Float): Float {
        return plotArea.bottom - ((y - bounds.yMin) / bounds.yRange) * plotArea.height
    }

    /**
     * Convert a data point to pixel coordinates.
     */
    fun dataToPixel(point: DataPoint): Offset {
        return Offset(dataToPixelX(point.x), dataToPixelY(point.y))
    }
}

/**
 * Main plot composable that provides a canvas with coordinate transformations,
 * optional grid, and axis rendering.
 *
 * @param bounds The data bounds for the plot
 * @param modifier Modifier for the plot canvas
 * @param padding Padding configuration for axis labels
 * @param xAxis X-axis configuration (null to hide)
 * @param yAxis Y-axis configuration (null to hide)
 * @param grid Grid configuration (null to hide)
 * @param content Content lambda with PlotScope for custom drawing
 */
@Composable
fun Plot(
    bounds: PlotBounds,
    modifier: Modifier = Modifier,
    padding: PlotPadding = PlotPadding(),
    xAxis: AxisConfig? = AxisConfig(),
    yAxis: AxisConfig? = AxisConfig(),
    grid: GridConfig? = GridConfig(),
    content: PlotScope.() -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember { TextStyle(color = (yAxis?.color ?: xAxis?.color ?: AxisConfig().color), fontSize = 11.sp) }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height

        // Calculate plot area
        val plotArea = Rect(
            left = padding.left,
            top = padding.top,
            right = totalWidth - padding.right,
            bottom = totalHeight - padding.bottom
        )

        val scope = PlotScope(bounds, plotArea, this, textMeasurer, textStyle)

        // Draw grid first (behind everything)
        grid?.let { scope.drawGrid(it) }

        // Draw axes
        yAxis?.let { scope.drawYAxis(it) }
        xAxis?.let { scope.drawXAxis(it) }

        // Draw user content
        scope.content()
    }
}

/**
 * Convenience composable for simple line plots.
 *
 * @param series List of data series to plot
 * @param modifier Modifier for the plot canvas
 * @param bounds Data bounds (auto-calculated if not specified)
 * @param padding Padding configuration
 * @param xAxis X-axis configuration
 * @param yAxis Y-axis configuration
 * @param grid Grid configuration
 */
@Composable
fun LinePlot(
    series: List<DataSeries>,
    modifier: Modifier = Modifier,
    bounds: PlotBounds = PlotBounds.auto(series),
    padding: PlotPadding = PlotPadding(),
    xAxis: AxisConfig? = AxisConfig(),
    yAxis: AxisConfig? = AxisConfig(),
    grid: GridConfig? = GridConfig()
) {
    Plot(
        bounds = bounds,
        modifier = modifier,
        padding = padding,
        xAxis = xAxis,
        yAxis = yAxis,
        grid = grid
    ) {
        series.forEach { dataSeries ->
            drawLineSeries(dataSeries)
        }
    }
}
