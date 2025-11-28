package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.io.Source
import kotlin.math.PI

@Composable
fun SinusSliderScreen() {
    val viewModel = remember { SinusSliderViewModel() }
    val modelLoadingState by viewModel.modelLoadingState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        // Visualization
        SinusVisualization(
            sliderValue = viewModel.sliderValue,
            actualSinus = viewModel.sinusValue,
            approximatedSinusKan = viewModel.modelSinusValueKan,
            approximatedSinusMlp = viewModel.modelSinusValueMlp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Values display
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header values spanning full width
                Text(
                    text = "Angle: ${viewModel.formattedAngle}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Actual sin: ${viewModel.formattedSinusValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Two columns: KAN on the left, MLP on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "KAN approximated sin: ${viewModel.formattedModelSinusValueKan}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "KAN error: ${viewModel.formattedErrorValueKan}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MLP approximated sin: ${viewModel.formattedModelSinusValueMlp}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "MLP error: ${viewModel.formattedErrorValueMlp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Neural Network Visualization
        if (modelLoadingState == ModelLoadingState.Success) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeuralNetworkVisualization(
                    model = viewModel.neuralNetworkModel,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Slider
        Slider(
            value = viewModel.sliderValue,
            onValueChange = { viewModel.updateSliderValue(it) },
            valueRange = 0f..(PI.toFloat() / 2),
            modifier = Modifier.fillMaxWidth()
        )

        // Model loading state
        when (modelLoadingState) {
            ModelLoadingState.Initial -> {
                Button(
                    onClick = { viewModel.loadModel() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Load Model")
                }
            }
            ModelLoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            ModelLoadingState.Success -> {
                Text(
                    text = "Model loaded successfully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            is ModelLoadingState.Error -> {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${(modelLoadingState as ModelLoadingState.Error).message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.loadModel() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
