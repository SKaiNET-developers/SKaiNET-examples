package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SinusVisualization(
    sliderValue: Float,
    actualSinus: Double,
    approximatedSinus: Double,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw x-axis
        drawLine(
            Color.Gray,
            Offset(0f, centerY),
            Offset(width, centerY),
            strokeWidth = 1f
        )

        // Draw actual sinus curve
        drawSinusCurve(Color.Gray.copy(alpha = 0.3f), width, height, centerY)

        // Draw vertical line at current x position
        val x = (sliderValue / (PI.toFloat() / 2)) * width
        drawLine(
            Color.Gray,
            Offset(x, 0f),
            Offset(x, height),
            strokeWidth = 1f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw points for actual and approximated values
        val actualY = centerY - (actualSinus * centerY).toFloat()
        val approximatedY = centerY - (approximatedSinus * centerY).toFloat()

        // Draw line between points to show error
        drawLine(
            error.copy(alpha = 0.5f),
            Offset(x, actualY),
            Offset(x, approximatedY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // Draw points
        drawCircle(primary, 6f, Offset(x, actualY))
        drawCircle(secondary, 6f, Offset(x, approximatedY))
    }
}

private fun DrawScope.drawSinusCurve(
    color: Color,
    width: Float,
    height: Float,
    centerY: Float,
    segments: Int = 100
) {
    val points = List(segments + 1) { i ->
        val x = i * (width / segments)
        val angle = (x / width) * (PI.toFloat() / 2)
        val y = centerY - (sin(angle.toDouble()) * centerY).toFloat()
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
