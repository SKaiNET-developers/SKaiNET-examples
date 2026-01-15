package sk.ai.net.samples.kmp.mnist.demo.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import sk.ai.net.samples.kmp.mnist.demo.training.MnistTrainingState
import sk.ai.net.samples.kmp.mnist.demo.training.MnistTrainingViewModel

@Composable
fun TrainingScreen(viewModel: MnistTrainingViewModel) {
    val trainingState by viewModel.trainingState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MNIST Training",
            style = MaterialTheme.typography.headlineMedium
        )

        // Training Configuration Card
        TrainingConfigCard(
            trainingState = trainingState,
            onStartTraining = { epochs, batchSize, lr ->
                viewModel.startTraining(epochs, batchSize, lr)
            },
            onStopTraining = { viewModel.stopTraining() }
        )

        // Progress Card
        TrainingProgressCard(trainingState)

        // Charts Card
        if (trainingState.lossHistory.isNotEmpty()) {
            TrainingChartsCard(trainingState)
        }
    }
}

@Composable
private fun TrainingConfigCard(
    trainingState: MnistTrainingState,
    onStartTraining: (epochs: Int, batchSize: Int, learningRate: Double) -> Unit,
    onStopTraining: () -> Unit
) {
    var epochs by remember { mutableStateOf("10") }
    var batchSize by remember { mutableStateOf("32") }
    var learningRate by remember { mutableStateOf("0.01") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Training Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = epochs,
                    onValueChange = { epochs = it.filter { c -> c.isDigit() } },
                    label = { Text("Epochs") },
                    modifier = Modifier.weight(1f),
                    enabled = !trainingState.isTraining,
                    singleLine = true
                )

                OutlinedTextField(
                    value = batchSize,
                    onValueChange = { batchSize = it.filter { c -> c.isDigit() } },
                    label = { Text("Batch Size") },
                    modifier = Modifier.weight(1f),
                    enabled = !trainingState.isTraining,
                    singleLine = true
                )

                OutlinedTextField(
                    value = learningRate,
                    onValueChange = { learningRate = it },
                    label = { Text("Learning Rate") },
                    modifier = Modifier.weight(1f),
                    enabled = !trainingState.isTraining,
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!trainingState.isTraining) {
                    Button(
                        onClick = {
                            val e = epochs.toIntOrNull() ?: 10
                            val b = batchSize.toIntOrNull() ?: 32
                            val lr = learningRate.toDoubleOrNull() ?: 0.01
                            onStartTraining(e, b, lr)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Training")
                    }
                } else {
                    Button(
                        onClick = onStopTraining,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Training")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingProgressCard(trainingState: MnistTrainingState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = trainingState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    trainingState.isCompleted -> Color(0xFF4CAF50)
                    trainingState.isTraining -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            LinearProgressIndicator(
                progress = { trainingState.epoch.toFloat() / trainingState.totalEpochs.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Epoch: ${trainingState.epoch}/${trainingState.totalEpochs}")
                Text("Loss: ${formatFloat(trainingState.currentLoss)}")
                Text("Accuracy: ${formatPercent(trainingState.currentAccuracy)}")
            }
        }
    }
}

@Composable
private fun TrainingChartsCard(trainingState: MnistTrainingState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Training Metrics",
                style = MaterialTheme.typography.titleMedium
            )

            // Loss Chart
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFF44336))
                    )
                    Text("Loss", style = MaterialTheme.typography.bodySmall)
                }
                LineChart(
                    data = trainingState.lossHistory,
                    color = Color(0xFFF44336),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            // Accuracy Chart
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50))
                    )
                    Text("Accuracy", style = MaterialTheme.typography.bodySmall)
                }
                LineChart(
                    data = trainingState.accuracyHistory,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxValue = 1f
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    maxValue: Float? = null
) {
    if (data.isEmpty()) return

    val actualMax = maxValue ?: data.maxOrNull() ?: 1f
    val actualMin = if (maxValue != null) 0f else (data.minOrNull() ?: 0f)
    val range = (actualMax - actualMin).coerceAtLeast(0.001f)

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val width = size.width
        val height = size.height
        val padding = 4.dp.toPx()

        if (data.size < 2) {
            // Just draw a point if only one data point
            val x = width / 2
            val y = height - padding - ((data[0] - actualMin) / range) * (height - 2 * padding)
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y))
            return@Canvas
        }

        val path = Path()
        val stepX = (width - 2 * padding) / (data.size - 1)

        data.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val normalizedValue = ((value - actualMin) / range).coerceIn(0f, 1f)
            val y = height - padding - normalizedValue * (height - 2 * padding)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun formatFloat(value: Float): String {
    return try {
        val factor = 10000.0
        val rounded = kotlin.math.round(value.toDouble() * factor) / factor
        rounded.toString()
    } catch (e: Exception) {
        value.toString()
    }
}

private fun formatPercent(value: Float): String {
    return try {
        val percent = (value * 100).toInt()
        "$percent%"
    } catch (e: Exception) {
        "${value * 100}%"
    }
}
