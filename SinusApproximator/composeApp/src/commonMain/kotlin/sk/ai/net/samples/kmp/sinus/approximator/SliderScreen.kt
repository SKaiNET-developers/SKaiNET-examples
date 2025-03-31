package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        // Slider für Werte von 0 bis PI/2
        Slider(
            value = viewModel.sliderValue,
            onValueChange = { viewModel.updateSliderValue(it) },
            valueRange = 0f..(PI.toFloat() / 2),
            modifier = Modifier.fillMaxWidth()
        )

        // Anzeigen des aktuellen Sliderwertes und des berechneten Sinuswertes
        Text(
            text = "Winkel: ${viewModel.sliderValue}",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Sinus: ${viewModel.sinusValue}",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Model Sinus: ${viewModel.modelSinusValue}",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(top = 16.dp)
        )

        when (modelLoadingState) {
            ModelLoadingState.Initial -> {
                Button(
                    onClick = { viewModel.loadModel() },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Load Model")
                }
            }
            ModelLoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            ModelLoadingState.Success -> {
                Text(
                    text = "Model loaded successfully",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            is ModelLoadingState.Error -> {
                Column(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${(modelLoadingState as ModelLoadingState.Error).message}",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error
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
