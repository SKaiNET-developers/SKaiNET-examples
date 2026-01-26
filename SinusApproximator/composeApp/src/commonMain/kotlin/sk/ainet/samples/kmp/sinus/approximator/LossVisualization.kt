package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LossVisualization(
    lossHistory: List<Float>,
    totalEpochs: Int,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.error
    val axisColor = Color.Gray
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember { TextStyle(color = Color.Gray, fontSize = 11.sp) }

    // Padding for axis labels
    val leftPadding = 50f
    val bottomPadding = 25f
    val topPadding = 10f
    val rightPadding = 10f

    Canvas(
        modifier = modifier
            .height(150.dp)
            .fillMaxWidth()
    ) {
        val totalWidth = size.width
        val totalHeight = size.height

        // Plot area dimensions
        val plotWidth = totalWidth - leftPadding - rightPadding
        val plotHeight = totalHeight - topPadding - bottomPadding
        val plotBottom = totalHeight - bottomPadding
        val plotTop = topPadding
        val plotLeft = leftPadding
        val plotRight = totalWidth - rightPadding

        // Draw Y-axis
        drawLine(
            axisColor,
            Offset(plotLeft, plotTop),
            Offset(plotLeft, plotBottom),
            strokeWidth = 1.5f
        )

        // Draw X-axis
        drawLine(
            axisColor,
            Offset(plotLeft, plotBottom),
            Offset(plotRight, plotBottom),
            strokeWidth = 1.5f
        )

        if (lossHistory.size < 2) {
            // Draw axis labels even with no data
            drawText(
                textMeasurer = textMeasurer,
                text = "0",
                topLeft = Offset(plotLeft - 3f, plotBottom + 4f),
                style = textStyle
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "$totalEpochs",
                topLeft = Offset(plotRight - 15f, plotBottom + 4f),
                style = textStyle
            )
            return@Canvas
        }

        val maxLoss = lossHistory.maxOrNull() ?: 1f
        val minLoss = lossHistory.minOrNull() ?: 0f
        val range = (maxLoss - minLoss).coerceAtLeast(0.0001f)

        // Helper functions for coordinate conversion
        fun valueToY(loss: Float): Float = plotTop + ((maxLoss - loss) / range * plotHeight)
        fun epochToX(epoch: Int): Float = plotLeft + (epoch.toFloat() / (totalEpochs - 1).coerceAtLeast(1) * plotWidth)

        // Y-axis tick marks (min, mid, max loss)
        val midLoss = (maxLoss + minLoss) / 2
        val yTicks = listOf(minLoss, midLoss, maxLoss)
        for (tick in yTicks) {
            val y = valueToY(tick)
            // Tick mark
            drawLine(
                axisColor,
                Offset(plotLeft - 5f, y),
                Offset(plotLeft, y),
                strokeWidth = 1.5f
            )
            // Grid line (light)
            if (tick != minLoss) {
                drawLine(
                    axisColor.copy(alpha = 0.2f),
                    Offset(plotLeft, y),
                    Offset(plotRight, y),
                    strokeWidth = 1f
                )
            }
        }

        // X-axis tick marks (0, mid, total epochs)
        val midEpoch = totalEpochs / 2
        val xTicks = listOf(0, midEpoch, totalEpochs)
        for (tick in xTicks) {
            val x = plotLeft + (tick.toFloat() / totalEpochs * plotWidth)
            // Tick mark
            drawLine(
                axisColor,
                Offset(x, plotBottom),
                Offset(x, plotBottom + 5f),
                strokeWidth = 1.5f
            )
            // Grid line (light) - only for middle
            if (tick == midEpoch) {
                drawLine(
                    axisColor.copy(alpha = 0.2f),
                    Offset(x, plotTop),
                    Offset(x, plotBottom),
                    strokeWidth = 1f
                )
            }
        }

        // Format loss values (cross-platform)
        fun formatLoss(loss: Float): String {
            val str = loss.toString()
            val dotIndex = str.indexOf('.')
            return if (dotIndex == -1) {
                str
            } else {
                val decimals = if (loss < 0.01f) 4 else if (loss < 1f) 3 else 2
                val endIndex = minOf(dotIndex + decimals + 1, str.length)
                str.substring(0, endIndex)
            }
        }

        // Y-axis labels
        drawText(
            textMeasurer = textMeasurer,
            text = formatLoss(maxLoss),
            topLeft = Offset(2f, valueToY(maxLoss) - 6f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = formatLoss(midLoss),
            topLeft = Offset(2f, valueToY(midLoss) - 6f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = formatLoss(minLoss),
            topLeft = Offset(2f, valueToY(minLoss) - 6f),
            style = textStyle
        )

        // X-axis labels (epochs)
        drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(plotLeft - 3f, plotBottom + 4f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "$midEpoch",
            topLeft = Offset(plotLeft + (midEpoch.toFloat() / totalEpochs * plotWidth) - 10f, plotBottom + 4f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "$totalEpochs",
            topLeft = Offset(plotRight - 15f, plotBottom + 4f),
            style = textStyle
        )

        // Draw loss curve
        val path = Path().apply {
            lossHistory.forEachIndexed { index, loss ->
                val x = epochToX(index)
                val y = valueToY(loss)
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
