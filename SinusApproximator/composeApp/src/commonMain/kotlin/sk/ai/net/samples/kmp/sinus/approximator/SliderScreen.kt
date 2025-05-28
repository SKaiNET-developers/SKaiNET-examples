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
fun SinusSliderScreen(handleSource: () -> Source) {
    val viewModel = remember { SinusSliderViewModel(handleSource) }
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
            approximatedSinus = viewModel.modelSinusValue,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Values display
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Angle: ${viewModel.formattedAngle}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Actual sin: ${viewModel.formattedSinusValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Approximated sin: ${viewModel.formattedModelSinusValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Error: ${viewModel.formattedErrorValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
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
