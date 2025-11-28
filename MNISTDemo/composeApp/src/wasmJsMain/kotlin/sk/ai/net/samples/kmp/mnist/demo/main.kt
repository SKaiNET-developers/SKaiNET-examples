package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.io.Buffer

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val resourcePath = "files/mnist_mlp.gguf"

    // Add debug logging
    println("Starting MNIST Demo application")
    println("Resource path: $resourcePath")

    try {
        ComposeViewport(document.body!!) {
            println("ComposeViewport initialized")
            val loadingState by ResourceUtils.loadingState.collectAsState()

            // Load the mnist.json resource when the app starts
            LaunchedEffect(Unit) {
                println("LaunchedEffect triggered, loading resource")
                try {
                    ResourceUtils.loadResource(resourcePath)
                    println("Resource loaded successfully")
                } catch (e: Exception) {
                    println("Error loading resource: ${e.message ?: "Unknown error (null message)"}")
                    e.printStackTrace()
                }
            }

            // Only show the app when the resource is loaded
            if (loadingState == LoadingState.Success) {
                println("LoadingState.Success, showing App")
                App {
                    // Provide a Source for the mnist.json file
                    ResourceUtils.getSourceFromResource(resourcePath) ?: Buffer()
                }
            } else if (loadingState is LoadingState.Error) {
                // Show error message
                val errorMessage = (loadingState as LoadingState.Error).message
                println("LoadingState.Error: $errorMessage")
                androidx.compose.material.Text("Error loading resource: $errorMessage")
            } else {
                println("LoadingState: $loadingState, showing loading indicator")
                // Show loading indicator
                androidx.compose.material.CircularProgressIndicator()
            }
        }
    } catch (e: Exception) {
        val errorMessage = e.message ?: "Unknown error (null message)"
        println("Error in main: $errorMessage")
        println("Exception details: ${e.toString()}")
        e.printStackTrace()
        window.alert("An error occurred: $errorMessage\nDetails: ${e.toString()}")
    }
}
