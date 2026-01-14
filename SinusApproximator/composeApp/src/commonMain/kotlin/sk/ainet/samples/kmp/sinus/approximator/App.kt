package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(0) }
    val trainingViewModel = remember { SinusTrainingViewModel() }

    MaterialTheme {
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
                }
            }
        ) { padding ->
            Surface(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> SinusSliderScreen(trainingViewModel)
                    1 -> SinusTrainingScreen(trainingViewModel)
                }
            }
        }
    }
}