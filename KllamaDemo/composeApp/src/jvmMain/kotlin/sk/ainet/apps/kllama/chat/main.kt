package sk.ainet.apps.kllama.chat

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator

fun main() = application {
    // Initialize ServiceLocator with common model loader
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.initialize(CommonModelLoader())
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "KLlama Chat - Offline LLM",
        state = rememberWindowState(size = DpSize(1024.dp, 768.dp))
    ) {
        App()
    }
}
