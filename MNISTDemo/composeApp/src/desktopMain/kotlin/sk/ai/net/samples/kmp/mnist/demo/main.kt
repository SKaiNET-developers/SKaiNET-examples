package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.io.Buffer
import mnistdemo.composeapp.generated.resources.Res
import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.di.ServiceLocator
import sk.ainet.clean.domain.factory.DigitClassifierFactory
import sk.ainet.clean.domain.factory.DigitClassifierFactoryImpl
import sk.ainet.clean.framework.inference.CnnInferenceModuleAdapter
import sk.ainet.clean.framework.inference.MlpInferenceModuleAdapter

fun main() {
    // Configure Clean Architecture ServiceLocator for Desktop platform (DI bootstrapping)
    // ResourceReader backed by Compose Multiplatform resources
    val desktopResourceReader = object : ResourceReader {
        override suspend fun read(path: String): ByteArray? = try {
            Res.readBytes(path)
        } catch (e: Exception) {
            null
        }
    }

    // Initialize repository with temporary dummy to avoid circular dependency
    // but in Desktop we actually don't have the circularity if we don't call it here.
    // The issue is that FactoryImpl needs repository, and repository needs localDataSource,
    // which needs resourceReader.

    // Manual wiring to avoid using ServiceLocator.modelWeightsRepository before configuration
    // OR we just provide a lazy provider to the factory.

    val factory: DigitClassifierFactory = DigitClassifierFactoryImpl(
        repositoryProvider = { ServiceLocator.modelWeightsRepository },
        cnnModuleProvider = { CnnInferenceModuleAdapter.create() },
        mlpModuleProvider = { MlpInferenceModuleAdapter.create() }
    )

    // Inject into ServiceLocator once at startup
    ServiceLocator.configure(
        resourceReader = desktopResourceReader,
        digitClassifierFactory = factory,
    )

    application {
        val resourcePath = "files/mnist_mlp.gguf"

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

    Window(
        onCloseRequest = ::exitApplication,
        title = "MNIST Demo",
    ) {

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
}
}