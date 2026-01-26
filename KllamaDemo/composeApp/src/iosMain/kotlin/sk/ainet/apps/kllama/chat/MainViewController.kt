package sk.ainet.apps.kllama.chat

import androidx.compose.ui.window.ComposeUIViewController
import sk.ainet.apps.kllama.chat.di.ServiceLocator

fun MainViewController() = ComposeUIViewController {
    // Initialize ServiceLocator with stub loader for iOS
    // (Local inference not fully supported on iOS yet)
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.initializeWithStub("iOS")
    }

    App()
}
