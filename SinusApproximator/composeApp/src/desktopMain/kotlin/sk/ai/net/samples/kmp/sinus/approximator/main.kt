package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sinus approximator",
    ) {
        App()
    }
}
