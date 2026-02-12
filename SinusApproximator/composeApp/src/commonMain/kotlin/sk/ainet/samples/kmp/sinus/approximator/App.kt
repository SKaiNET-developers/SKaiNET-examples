package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.ui.theme.SKaiNETTheme
import sk.ainet.ui.theme.ThemeController
import sk.ainet.ui.components.SKaiNETProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(0) }
    val sliderViewModel = remember { SinusSliderViewModel() }
    val trainingViewModel = remember { SinusTrainingViewModel() }

    // Create theme controller only for WASM platform
    val themeController = remember { if (isWasmPlatform) ThemeController() else null }

    SKaiNETTheme(themeController = themeController) {
        Scaffold(
            topBar = {
                // Show theme toggle only on WASM
                if (themeController != null) {
                    TopAppBar(
                        title = { Text("Sinus Approximator") },
                        actions = {
                            IconButton(onClick = { themeController.toggleTheme() }) {
                                Text(if (themeController.isDarkTheme) "☀️" else "🌙")
                            }
                        }
                    )
                }
            },
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

                            when (modelLoadingState) {
                                ModelLoadingState.Initial -> {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(
                                        onClick = { sliderViewModel.loadModel() }
                                    ) {
                                        Text("Load Model")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                ModelLoadingState.Loading -> {
                                    Spacer(modifier = Modifier.weight(1f))
                                    SKaiNETProgressIndicator()
                                    Text(
                                        text = "Loading model...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                ModelLoadingState.Success -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        NeuralNetworkVisualization(
                                            model = sliderViewModel.neuralNetworkModel,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                is ModelLoadingState.Error -> {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Error: ${(modelLoadingState as ModelLoadingState.Error).message}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(
                                        onClick = { sliderViewModel.loadModel() },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text("Retry")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}