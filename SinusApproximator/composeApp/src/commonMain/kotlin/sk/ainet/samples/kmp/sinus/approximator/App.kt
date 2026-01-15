package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.sinus.approximator.ui.SKaiNETTheme

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(0) }
    val sliderViewModel = remember { SinusSliderViewModel() }
    val trainingViewModel = remember { SinusTrainingViewModel() }

    SKaiNETTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Approximation") },
                        icon = { Text("A") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Training") },
                        icon = { Text("T") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Model") },
                        icon = { Text("M") }
                    )
                }
            }
        ) { padding ->
            Surface(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> SinusSliderScreen(sliderViewModel, trainingViewModel)
                    1 -> SinusTrainingScreen(trainingViewModel)
                    2 -> {
                        val modelLoadingState by sliderViewModel.modelLoadingState.collectAsState()
                        if (modelLoadingState == ModelLoadingState.Success) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Model Visualization",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    NeuralNetworkVisualization(
                                        model = sliderViewModel.neuralNetworkModel,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Please load the model in the Approximation tab first.")
                            }
                        }
                    }
                }
            }
        }
    }
}