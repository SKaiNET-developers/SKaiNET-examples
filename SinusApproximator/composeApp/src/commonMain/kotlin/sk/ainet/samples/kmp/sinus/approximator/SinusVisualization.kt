package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SinusVisualization(
    sliderValue: Float,
    actualSinus: Double,
    approximatedSinusKan: Float,
    approximatedSinusMlp: Float,
    approximatedSinusPretrained: Float? = null,
    approximatedSinusTrained: Float? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val pretrainedColor = Color(0xFF673AB7) // Deep Purple for pretrained
    val trainedColor = Color(0xFF4CAF50) // Green for trained
    val axisColor = Color.Gray
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember { TextStyle(color = Color.Gray, fontSize = 11.sp) }

    // Padding for axis labels
    val leftPadding = 40f
    val bottomPadding = 25f
    val topPadding = 10f
    val rightPadding = 10f

    Canvas(
        modifier = modifier
            .height(220.dp)
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

        // Y-axis: 0 at bottom, 1 at top (sine values from 0 to 1)
        fun valueToY(value: Float): Float = plotBottom - (value * plotHeight)
        fun xToPlot(normalizedX: Float): Float = plotLeft + (normalizedX * plotWidth)

        // Draw Y-axis
        drawLine(
            axisColor,
            Offset(plotLeft, plotTop),
            Offset(plotLeft, plotBottom),
            strokeWidth = 1.5f
        )

        // Draw X-axis (at y=0, which is the bottom of the plot)
        drawLine(
            axisColor,
            Offset(plotLeft, plotBottom),
            Offset(plotRight, plotBottom),
            strokeWidth = 1.5f
        )

        // Y-axis tick marks and labels (0, 0.5, 1.0)
        val yTicks = listOf(0f, 0.5f, 1f)
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
            if (tick > 0f) {
                drawLine(
                    axisColor.copy(alpha = 0.2f),
                    Offset(plotLeft, y),
                    Offset(plotRight, y),
                    strokeWidth = 1f
                )
            }
        }

        // X-axis tick marks (0, π/4, π/2)
        val xTicks = listOf(0f, 0.5f, 1f) // Normalized positions
        for (tick in xTicks) {
            val x = xToPlot(tick)
            // Tick mark
            drawLine(
                axisColor,
                Offset(x, plotBottom),
                Offset(x, plotBottom + 5f),
                strokeWidth = 1.5f
            )
            // Grid line (light)
            if (tick > 0f && tick < 1f) {
                drawLine(
                    axisColor.copy(alpha = 0.2f),
                    Offset(x, plotTop),
                    Offset(x, plotBottom),
                    strokeWidth = 1f
                )
            }
        }

        // Draw axis labels using cross-platform TextMeasurer
        // Y-axis labels
        drawText(
            textMeasurer = textMeasurer,
            text = "1.0",
            topLeft = Offset(plotLeft - 32f, valueToY(1f) - 6f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "0.5",
            topLeft = Offset(plotLeft - 32f, valueToY(0.5f) - 6f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(plotLeft - 15f, valueToY(0f) - 6f),
            style = textStyle
        )

        // X-axis labels
        drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(xToPlot(0f) - 3f, plotBottom + 4f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "π/4",
            topLeft = Offset(xToPlot(0.5f) - 10f, plotBottom + 4f),
            style = textStyle
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "π/2",
            topLeft = Offset(xToPlot(1f) - 10f, plotBottom + 4f),
            style = textStyle
        )

        // Draw actual sinus curve (in plot coordinates)
        drawSinusCurveInPlot(Color.Gray.copy(alpha = 0.3f), plotLeft, plotWidth, plotBottom, plotHeight)

        // Draw vertical line at current x position
        val normalizedX = sliderValue / (PI.toFloat() / 2)
        val x = xToPlot(normalizedX)
        drawLine(
            Color.Gray,
            Offset(x, plotTop),
            Offset(x, plotBottom),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw points for actual and approximated values
        val actualY = valueToY(actualSinus.toFloat())
        val approximatedYKan = valueToY(approximatedSinusKan)
        val approximatedYMlp = valueToY(approximatedSinusMlp)

        // Draw lines between points to show errors (use same colors as the dots)
        drawLine(
            secondary.copy(alpha = 0.5f),
            Offset(x, actualY),
            Offset(x, approximatedYKan),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            tertiary.copy(alpha = 0.5f),
            Offset(x, actualY),
            Offset(x, approximatedYMlp),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        if (approximatedSinusPretrained != null) {
            val approximatedYPretrained = valueToY(approximatedSinusPretrained)
            drawLine(
                pretrainedColor.copy(alpha = 0.5f),
                Offset(x, actualY),
                Offset(x, approximatedYPretrained),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            drawCircle(pretrainedColor, 6f, Offset(x, approximatedYPretrained))
        }

        if (approximatedSinusTrained != null) {
            val approximatedYTrained = valueToY(approximatedSinusTrained)
            drawLine(
                trainedColor.copy(alpha = 0.5f),
                Offset(x, actualY),
                Offset(x, approximatedYTrained),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            drawCircle(trainedColor, 6f, Offset(x, approximatedYTrained))
        }

        // Draw points
        drawCircle(primary, 6f, Offset(x, actualY))
        // KAN dot
        drawCircle(secondary, 6f, Offset(x, approximatedYKan))
        // MLP dot
        drawCircle(tertiary, 6f, Offset(x, approximatedYMlp))
    }
}

private fun DrawScope.drawSinusCurveInPlot(
    color: Color,
    plotLeft: Float,
    plotWidth: Float,
    plotBottom: Float,
    plotHeight: Float,
    segments: Int = 100
) {
    val points = List(segments + 1) { i ->
        val normalizedX = i.toFloat() / segments
        val x = plotLeft + (normalizedX * plotWidth)
        val angle = normalizedX * (PI.toFloat() / 2)
        val sinValue = sin(angle.toDouble()).toFloat()
        val y = plotBottom - (sinValue * plotHeight)
        Offset(x, y)
    }

    for (i in 0 until points.lastIndex) {
        drawLine(
            color,
            points[i],
            points[i + 1],
            strokeWidth = 2f
        )
    }
}
