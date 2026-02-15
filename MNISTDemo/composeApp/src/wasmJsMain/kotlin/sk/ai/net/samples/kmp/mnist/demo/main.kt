package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.io.Buffer
import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.di.ServiceLocator
import sk.ainet.clean.domain.factory.DigitClassifierFactory
import sk.ainet.clean.domain.factory.DigitClassifierFactoryImpl
import sk.ainet.clean.framework.inference.CnnInferenceModuleAdapter
import sk.ainet.clean.framework.inference.MlpInferenceModuleAdapter
import mnistdemo.composeapp.generated.resources.Res
import sk.ai.net.samples.kmp.mnist.demo.navigation.NavigationHost
import sk.ai.net.samples.kmp.mnist.demo.navigation.Screen
import sk.ai.net.samples.kmp.mnist.demo.navigation.rememberNavigationState
import sk.ai.net.samples.kmp.mnist.demo.screens.HomeScreen
import sk.ai.net.samples.kmp.mnist.demo.screens.SettingsScreen
import sk.ai.net.samples.kmp.mnist.demo.screens.TrainingScreen
import sk.ai.net.samples.kmp.mnist.demo.training.MnistTrainingViewModel
import sk.ai.net.samples.kmp.mnist.demo.ui.LocalHandleSource
import sk.ai.net.samples.kmp.mnist.demo.ui.ResponsiveLayout
import sk.ai.net.samples.kmp.mnist.demo.ui.WindowSizeClass
import sk.ai.net.samples.kmp.mnist.demo.ui.isLandscape

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Configure Clean Architecture ServiceLocator for WASM platform (DI bootstrapping)
    val wasmResourceReader = object : ResourceReader {
        override suspend fun read(path: String): ByteArray? = try {
            Res.readBytes(path)
        } catch (e: Exception) {
            null
        }
    }

    val factory: DigitClassifierFactory = DigitClassifierFactoryImpl(
        repositoryProvider = { ServiceLocator.modelWeightsRepository },
        cnnModuleProvider = { CnnInferenceModuleAdapter.create() },
        mlpModuleProvider = { MlpInferenceModuleAdapter.create() }
    )

    ServiceLocator.configure(
        resourceReader = wasmResourceReader,
        digitClassifierFactory = factory,
    )

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
                val handleSource: () -> kotlinx.io.Source = { ResourceUtils.getSourceFromResource(resourcePath) ?: Buffer() }
                
                // Provide the handleSource function to all composables in the app
                CompositionLocalProvider(LocalHandleSource provides handleSource) {
                    App(handleSource)
                }
            } else if (loadingState is LoadingState.Error) {
                // Show error message
                val errorMessage = (loadingState as LoadingState.Error).message
                println("LoadingState.Error: $errorMessage")
                androidx.compose.material.Text("Error loading resource: $errorMessage")
            } else {
                println("LoadingState: $loadingState, showing loading indicator")
                // Show loading indicator
                sk.ainet.ui.components.LoadingIndicator()
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
