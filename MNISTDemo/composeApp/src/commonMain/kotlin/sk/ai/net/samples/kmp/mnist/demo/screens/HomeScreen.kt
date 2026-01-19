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

import sk.ai.net.samples.kmp.mnist.demo.settings.AppSettings
import sk.ainet.clean.domain.model.ModelId
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import sk.ai.net.samples.kmp.mnist.demo.settings.ModelStatus
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import sk.ainet.clean.BuildInfo

/**
 * Home screen with app description and mode selection
 */
@Composable
fun HomeScreen(
    onGetStarted: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val selectedModel by AppSettings.selectedModelId.collectAsState(initial = ModelId.CNN_MNIST)
    val modelStatuses by AppSettings.modelStatuses.collectAsState()
    val status = modelStatuses[selectedModel] ?: ModelStatus.PRETRAINED

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
                    FeatureItem("Switch between MLP and CNN models")
                    FeatureItem("Train models locally")
                }
            }
        }

        // Active Model Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Model Info",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    val statusColor = if (status == ModelStatus.RETRAINED) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    val statusText = if (status == ModelStatus.RETRAINED) "Custom Trained" else "GGUF Pretrained"
                    
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                val modelName = if (selectedModel == ModelId.CNN_MNIST) "CNN (Convolutional)" else "MLP (Multi-Layer Perceptron)"
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                val architectureDesc = if (selectedModel == ModelId.CNN_MNIST) {
                    "Uses 2D convolution layers to extract spatial features from the image, followed by pooling and dense layers for classification."
                } else {
                    "A dense neural network where every input pixel (784 total) is connected to hidden layers of neurons."
                }
                
                Text(
                    text = architectureDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
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

        // Powered by SKaiNET
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Powered by SKaiNET ${BuildInfo.SKAINET_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
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
