package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkon.kmp.mnist.demo.ADigitClassifier
import com.kkon.kmp.mnist.demo.DigitClassifier
import kotlinx.coroutines.launch
import kotlinx.io.Source

/**
 * A simplified version of the App UI for the MNIST demo.
 * This is used to ensure basic functionality works before adding more complex features.
 */
@Composable
fun SimpleApp(handleSource: () -> Source) {
    val digitClassifier = remember { ADigitClassifier(handleSource) }
    var modelLoaded by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Welcome to MNIST Demo") }
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                statusMessage = "Loading model..."
                                println("SimpleApp: Starting to load model...")
                                digitClassifier.loadModel()
                                modelLoaded = true
                                println("SimpleApp: Model loaded successfully!")
                                statusMessage = "Model loaded successfully!"
                            } catch (e: Exception) {
                                val errorMessage = e.message ?: "Unknown error"
                                println("SimpleApp: Error loading model: $errorMessage")
                                println("SimpleApp: Exception details: ${e.toString()}")
                                e.printStackTrace()
                                statusMessage = "Error loading model: $errorMessage"
                            }
                        }
                    },
                    enabled = !modelLoaded
                ) {
                    Text(if (modelLoaded) "Model Loaded" else "Load Model")
                }

                if (modelLoaded) {
                    Button(
                        onClick = {
                            // Simple test of the classifier with a dummy image
                            val dummyImage = DigitClassifier.GrayScale28To28Image()
                            val result = digitClassifier.classify(dummyImage)
                            statusMessage = "Classification result: $result"
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Test Classification")
                    }
                }
            }
        }
    }
}
