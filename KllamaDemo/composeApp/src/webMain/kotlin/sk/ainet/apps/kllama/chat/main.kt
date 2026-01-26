package sk.ainet.apps.kllama.chat

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import sk.ainet.apps.kllama.chat.di.ServiceLocator

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize ServiceLocator with stub loader for Web
    // (Local inference experimental on Web/Wasm)
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.initializeWithStub("Web")
    }

    ComposeViewport {
        App()
    }
}
