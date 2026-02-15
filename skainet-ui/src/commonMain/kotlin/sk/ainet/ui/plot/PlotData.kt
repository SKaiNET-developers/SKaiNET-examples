package sk.ainet.ui.plot

import androidx.compose.ui.graphics.Color

/**
 * Represents a single data point in a plot.
 */
data class DataPoint(val x: Float, val y: Float)

/**
 * Represents a series of data points to be plotted as a line.
 */
data class DataSeries(
    val points: List<DataPoint>,
    val color: Color = Color.Unspecified,
    val strokeWidth: Float = 2f,
    val showPoints: Boolean = false,
    val pointRadius: Float = 4f
)

/**
 * Represents the data bounds (range) for a plot.
 */
data class PlotBounds(
    val xMin: Float,
    val xMax: Float,
    val yMin: Float,
    val yMax: Float
) {
    val xRange: Float get() = (xMax - xMin).coerceAtLeast(0.0001f)
    val yRange: Float get() = (yMax - yMin).coerceAtLeast(0.0001f)

    companion object {
        /**
         * Automatically calculate bounds from a list of data series.
         */
        fun auto(series: List<DataSeries>): PlotBounds {
            val allPoints = series.flatMap { it.points }
            if (allPoints.isEmpty()) {
                return PlotBounds(0f, 1f, 0f, 1f)
            }

            val xMin = allPoints.minOf { it.x }
            val xMax = allPoints.maxOf { it.x }
            val yMin = allPoints.minOf { it.y }
            val yMax = allPoints.maxOf { it.y }

            return PlotBounds(xMin, xMax, yMin, yMax)
        }
    }
}
