package sk.ainet.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp

/**
 * Draw a line series on the plot.
 */
fun PlotScope.drawLineSeries(series: DataSeries) {
    if (series.points.size < 2) return

    val color = if (series.color == Color.Unspecified) Color.Blue else series.color

    val path = Path().apply {
        series.points.forEachIndexed { index, point ->
            val pixel = dataToPixel(point)
            if (index == 0) {
                moveTo(pixel.x, pixel.y)
            } else {
                lineTo(pixel.x, pixel.y)
            }
        }
    }

    with(drawScope) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = series.strokeWidth.dp.toPx())
        )
    }

    // Draw points if enabled
    if (series.showPoints) {
        series.points.forEach { point ->
            val pixel = dataToPixel(point)
            drawScope.drawCircle(
                color = color,
                radius = series.pointRadius,
                center = pixel
            )
        }
    }
}

/**
 * Draw grid lines based on configuration.
 */
fun PlotScope.drawGrid(config: GridConfig) {
    // Horizontal grid lines
    if (config.showHorizontal) {
        val yTickCount = 3 // Same as default axis tick count
        for (i in 1 until yTickCount) {
            val yValue = bounds.yMin + (bounds.yRange * i / (yTickCount - 1))
            val y = dataToPixelY(yValue)
            drawScope.drawLine(
                color = config.color,
                start = Offset(plotArea.left, y),
                end = Offset(plotArea.right, y),
                strokeWidth = config.strokeWidth
            )
        }
    }

    // Vertical grid lines
    if (config.showVertical) {
        val xTickCount = 3 // Same as default axis tick count
        for (i in 1 until xTickCount - 1) {
            val xValue = bounds.xMin + (bounds.xRange * i / (xTickCount - 1))
            val x = dataToPixelX(xValue)
            drawScope.drawLine(
                color = config.color,
                start = Offset(x, plotArea.top),
                end = Offset(x, plotArea.bottom),
                strokeWidth = config.strokeWidth
            )
        }
    }
}

/**
 * Draw the X-axis with optional ticks and labels.
 */
fun PlotScope.drawXAxis(config: AxisConfig) {
    if (!config.show) return

    // Draw axis line
    drawScope.drawLine(
        color = config.color,
        start = Offset(plotArea.left, plotArea.bottom),
        end = Offset(plotArea.right, plotArea.bottom),
        strokeWidth = config.strokeWidth
    )

    // Draw ticks and labels
    if (config.showTicks || config.showLabels) {
        for (i in 0 until config.tickCount) {
            val xValue = bounds.xMin + (bounds.xRange * i / (config.tickCount - 1))
            val x = dataToPixelX(xValue)

            if (config.showTicks) {
                drawScope.drawLine(
                    color = config.color,
                    start = Offset(x, plotArea.bottom),
                    end = Offset(x, plotArea.bottom + config.tickLength),
                    strokeWidth = config.strokeWidth
                )
            }

            if (config.showLabels) {
                val label = config.labelFormatter(xValue)
                val textLayoutResult = textMeasurer.measure(label, textStyle)
                val textWidth = textLayoutResult.size.width
                drawScope.drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(x - textWidth / 2, plotArea.bottom + config.tickLength + 2f),
                    style = textStyle
                )
            }
        }
    }
}

/**
 * Draw the Y-axis with optional ticks and labels.
 */
fun PlotScope.drawYAxis(config: AxisConfig) {
    if (!config.show) return

    // Draw axis line
    drawScope.drawLine(
        color = config.color,
        start = Offset(plotArea.left, plotArea.top),
        end = Offset(plotArea.left, plotArea.bottom),
        strokeWidth = config.strokeWidth
    )

    // Draw ticks and labels
    if (config.showTicks || config.showLabels) {
        for (i in 0 until config.tickCount) {
            val yValue = bounds.yMin + (bounds.yRange * i / (config.tickCount - 1))
            val y = dataToPixelY(yValue)

            if (config.showTicks) {
                drawScope.drawLine(
                    color = config.color,
                    start = Offset(plotArea.left - config.tickLength, y),
                    end = Offset(plotArea.left, y),
                    strokeWidth = config.strokeWidth
                )
            }

            if (config.showLabels) {
                val label = config.labelFormatter(yValue)
                val textLayoutResult = textMeasurer.measure(label, textStyle)
                val textWidth = textLayoutResult.size.width
                val textHeight = textLayoutResult.size.height
                drawScope.drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(plotArea.left - config.tickLength - textWidth - 2f, y - textHeight / 2),
                    style = textStyle
                )
            }
        }
    }
}
