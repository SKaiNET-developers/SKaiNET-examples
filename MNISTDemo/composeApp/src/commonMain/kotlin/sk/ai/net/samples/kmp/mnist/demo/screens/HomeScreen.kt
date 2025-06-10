package sk.ai.net.samples.kmp.mnist.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Home screen with app description and mode selection
 */
@Composable
fun HomeScreen(
    onGetStarted: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title
        Text(
            text = "MNIST Digit Recognition",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // App Description
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About This App",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "This application demonstrates handwritten digit recognition using a neural network trained on the MNIST dataset.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "You can either draw a digit using the drawing canvas or select from sample images to test the recognition capabilities.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Features Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleLarge
                )

                // Feature list
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FeatureItem("Draw digits on a canvas")
                    FeatureItem("Select from sample images")
                    FeatureItem("Real-time digit recognition")
                    FeatureItem("Configurable settings")
                    FeatureItem("Works on multiple platforms")
                }
            }
        }

        // Get Started Button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(0.7f)
        ) {
            Text("Get Started")
        }

        // Technical Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Technical Information",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "This app is built using Kotlin Multiplatform with Compose Multiplatform for the UI. " +
                            "The neural network model is trained on the MNIST dataset, which contains 60,000 training " +
                            "images and 10,000 testing images of handwritten digits.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bullet point
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge
        )

        // Feature text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
