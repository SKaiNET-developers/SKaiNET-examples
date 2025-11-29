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
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier

fun main() = application {

    // Configure Clean Architecture ServiceLocator for Desktop platform (DI bootstrapping)
    run {
        // ResourceReader backed by Compose Multiplatform resources
        val desktopResourceReader = object : ResourceReader {
            override suspend fun read(path: String): ByteArray? = try {
                Res.readBytes(path)
            } catch (e: Exception) {
                null
            }
        }

        // Minimal factory that returns a simple classifier backed by a dummy inference module.
        // This avoids platform-specific NN dependencies here; proper adapters can replace this later.
        val factory: DigitClassifierFactory = object : DigitClassifierFactory {
            override fun create(modelId: ModelId): DigitClassifier {
                return object : DigitClassifier {
                    private var loaded = false
                    override suspend fun loadModel(modelId: ModelId) {
                        // Touch the repository to honor contract; ignore bytes for dummy impl
                        ServiceLocator.modelWeightsRepository.getWeights(modelId)
                        loaded = true
                    }

                    override fun classify(image: sk.ainet.clean.data.image.GrayScale28To28Image): Int {
                        check(loaded) { "Model weights not loaded; call loadModel() before classify()" }
                        var acc = 0.0
                        for (y in 0 until 28) for (x in 0 until 28) acc += image.getPixel(x, y)
                        val digit = (acc.toInt() % 10 + 10) % 10
                        return digit
                    }
                }
            }
        }

        // Inject into ServiceLocator once at startup
        ServiceLocator.configure(
            resourceReader = desktopResourceReader,
            digitClassifierFactory = factory,
        )
    }

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