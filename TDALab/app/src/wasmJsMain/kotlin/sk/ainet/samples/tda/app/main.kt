package sk.ainet.samples.tda.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // The static loading placeholder from index.html is no longer needed once Compose takes over.
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        App()
    }
}
