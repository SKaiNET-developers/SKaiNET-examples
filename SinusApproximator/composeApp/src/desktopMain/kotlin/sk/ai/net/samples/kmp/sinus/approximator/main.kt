package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.io.Buffer

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sinus approximator",
    ) {
        val loadingState by ResourceUtils.loadingState.collectAsState()

        // Load the sinus.json resource when the app starts
        LaunchedEffect(Unit) {
            ResourceUtils.loadResource("files/sinus.json")
        }

        // Only show the app when the resource is loaded
        if (loadingState == LoadingState.Success) {
            App {
                // Provide a Source for the sinus.json file
                ResourceUtils.getSourceFromResource("files/sinus.json") ?: Buffer()
            }
        } else if (loadingState is LoadingState.Error) {
            // Show error message
            val errorMessage = (loadingState as LoadingState.Error).message
            Text("Error loading resource: $errorMessage")
        } else {
            // Show loading indicator
            CircularProgressIndicator()
        }
    }
}
