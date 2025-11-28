package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * A composable that visualizes a dense neural network structure.
 * 
 * @param model The neural network model to visualize
 * @param modifier Modifier for the composable
 */
@Composable
fun NeuralNetworkVisualization(
    model: Module<FP32,Float>?,
    modifier: Modifier = Modifier
) {
    if (model == null) {
        Box(
            modifier = modifier
                .height(200.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Model not loaded",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Define the structure of the SineNN model
    // This is hardcoded for now, but could be made dynamic based on model inspection
    val layers = listOf(1, 16, 16, 1) // Input, Hidden1, Hidden2, Output
    val layerNames = listOf("Input", "Hidden 1", "Hidden 2", "Output")

    // Get colors from the theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Neural Network Structure",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height

            // Calculate positions for each layer
            val layerWidth = width / layers.size

            // Draw connections first (so they appear behind neurons)
            for (layerIdx in 0 until layers.size - 1) {
                val startX = layerWidth * (layerIdx + 0.5f)
                val endX = layerWidth * (layerIdx + 1.5f)

                val startLayerNeurons = layers[layerIdx]
                val endLayerNeurons = layers[layerIdx + 1]

                // Calculate neuron heights with additional spacing for hidden layers
                val startNeuronHeight = if (layerIdx == 0) {
                    // For input layer, use standard spacing
                    height / (startLayerNeurons + 1)
                } else {
                    // For hidden layers, increase spacing
                    height / (startLayerNeurons * 0.6f + 1)
                }

                val endNeuronHeight = if (layerIdx + 1 == layers.size - 1) {
                    // For output layer, use standard spacing
                    height / (endLayerNeurons + 1)
                } else {
                    // For hidden layers, increase spacing
                    height / (endLayerNeurons * 0.6f + 1)
                }

                // Calculate vertical offsets to center neurons
                val startTotalNeuronsHeight = startNeuronHeight * startLayerNeurons
                val startVerticalOffset = (height - startTotalNeuronsHeight) / 2

                val endTotalNeuronsHeight = endNeuronHeight * endLayerNeurons
                val endVerticalOffset = (height - endTotalNeuronsHeight) / 2

                for (startNeuron in 1..startLayerNeurons) {
                    // Center neurons vertically by adding the vertical offset
                    val startY = startVerticalOffset + startNeuronHeight * (startNeuron - 0.5f)

                    for (endNeuron in 1..endLayerNeurons) {
                        // Center neurons vertically by adding the vertical offset
                        val endY = endVerticalOffset + endNeuronHeight * (endNeuron - 0.5f)

                        // Draw connection line
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // Draw neurons for each layer
            for (layerIdx in layers.indices) {
                val layerX = layerWidth * (layerIdx + 0.5f)
                val neuronsInLayer = layers[layerIdx]

                // Calculate neuron height with additional spacing for hidden layers
                val neuronHeight = if (layerIdx == 0 || layerIdx == layers.size - 1) {
                    // For input and output layers, use standard spacing
                    height / (neuronsInLayer + 1)
                } else {
                    // For hidden layers, increase spacing by using a larger divisor
                    // This will make neurons less tightly packed
                    height / (neuronsInLayer * 0.6f + 1)
                }

                // Calculate vertical offset to center neurons
                val totalNeuronsHeight = neuronHeight * neuronsInLayer
                val verticalOffset = (height - totalNeuronsHeight) / 2

                // Draw neurons
                for (neuron in 1..neuronsInLayer) {
                    // Center neurons vertically by adding the vertical offset
                    val neuronY = verticalOffset + neuronHeight * (neuron - 0.5f)

                    // Choose color based on layer
                    val color = when (layerIdx) {
                        0 -> primaryColor
                        layers.size - 1 -> secondaryColor
                        else -> tertiaryColor
                    }

                    // Draw neuron circle
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = Offset(layerX, neuronY)
                    )
                }
            }
        }

        // Layer information
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-50).dp), // Move up more to avoid covering bottom neurons
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in layers.indices) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = layerNames[i],
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${layers[i]} neurons",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
