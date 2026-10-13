package sk.ainet.samples.emoji

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.model.DesktopLeafProvider
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SKaiNET Semantic Emoji Filter", state = rememberWindowState(width = 1100.dp, height = 800.dp)) {
        val viewModel = remember {
            val calibration = kotlinx.coroutines.runBlocking { CatalogResources.calibration() }
            EmojiSearchViewModel(
                loadEngine = { onProgress ->
                    val source = DesktopLeafProvider().resolve(onProgress)
                    LeafEmbeddingEngine.load(source, onProgress = onProgress)
                },
                loadIndex = { engine, onProgress -> CatalogResources.index(CatalogResources.catalog(), engine, onProgress = onProgress) },
                calibration = calibration,
                verifier = createVerifier(),
            )
        }
        App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
    }
}
