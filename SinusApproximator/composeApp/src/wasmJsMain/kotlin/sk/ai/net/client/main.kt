package sk.ai.net.client

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.io.Buffer
import sk.ai.net.samples.kmp.sinus.approximator.App
import sk.ai.net.samples.kmp.sinus.approximator.LoadingState
import sk.ai.net.samples.kmp.sinus.approximator.ResourceUtils

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
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
            androidx.compose.material3.Text("Error loading resource: $errorMessage")
        } else {
            // Show loading indicator
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}
