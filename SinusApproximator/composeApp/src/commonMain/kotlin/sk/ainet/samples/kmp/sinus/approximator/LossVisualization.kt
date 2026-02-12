package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.ui.plot.*

@Composable
fun LossVisualization(
    lossHistory: List<Float>,
    totalEpochs: Int,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.error

    // Create data points from loss history
    val points = lossHistory.mapIndexed { index, loss ->
        DataPoint(index.toFloat(), loss)
    }

    // Calculate bounds
    val bounds = if (lossHistory.size >= 2) {
        val maxLoss = lossHistory.maxOrNull() ?: 1f
        val minLoss = lossHistory.minOrNull() ?: 0f
        PlotBounds(0f, (totalEpochs - 1).toFloat().coerceAtLeast(1f), minLoss, maxLoss)
    } else {
        PlotBounds(0f, totalEpochs.toFloat().coerceAtLeast(1f), 0f, 1f)
    }

    LinePlot(
        series = listOf(DataSeries(points, color)),
        modifier = modifier.height(150.dp).fillMaxWidth(),
        bounds = bounds,
        padding = PlotPadding(left = 50f, right = 10f, top = 10f, bottom = 25f),
        xAxis = AxisConfig(
            tickCount = 3,
            labelFormatter = { it.toInt().toString() }
        ),
        yAxis = AxisConfig(
            tickCount = 3,
            labelFormatter = ::formatLoss
        )
    )
}

/**
 * Format loss values for display (cross-platform).
 */
private fun formatLoss(loss: Float): String {
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
