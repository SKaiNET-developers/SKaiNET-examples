package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LossVisualization(
    lossHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .height(150.dp)
            .fillMaxWidth()
    ) {
        if (lossHistory.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        
        val maxLoss = lossHistory.maxOrNull() ?: 1f
        val minLoss = lossHistory.minOrNull() ?: 0f
        val range = (maxLoss - minLoss).coerceAtLeast(0.0001f)

        val path = Path().apply {
            lossHistory.forEachIndexed { index, loss ->
                val x = index.toFloat() / (lossHistory.size - 1) * width
                val y = height - ((loss - minLoss) / range * height)
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
        
        // Draw baseline
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1f
        )
    }
}
