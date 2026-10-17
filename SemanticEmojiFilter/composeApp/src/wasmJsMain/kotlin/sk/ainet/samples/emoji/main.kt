package sk.ainet.samples.emoji

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import semanticemojifilter.composeapp.generated.resources.Res
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.engine.LeafFiles
import sk.ainet.samples.emoji.engine.LeafSource
import sk.ainet.samples.emoji.platform.installPlatformKernels
import sk.ainet.samples.emoji.ui.LocalEmojiFontFamily
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel

/**
 * The web build has no filesystem: the checkpoint is bundled as Compose resources (see
 * scripts/fetch-model.sh) and read into memory once. The catalog embeddings are the shipped
 * precomputed file, so the page is interactive right after the encoder is built. Skiko has no
 * system fonts in a browser, so the emoji glyphs come from a bundled Noto Color Emoji subset
 * (scripts/subset-emoji-font.sh); until it is read the grid would show replacement boxes.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        // The shipped logistic fit (catalog/calibration.json), the same one desktop and Android
        // load; the uncalibrated default would keep every confidence far below the top-tier cut.
        val calibration by produceState<Calibration?>(null) { value = CatalogResources.calibration() }
        val emojiFont by produceState<FontFamily>(FontFamily.Default) {
            value = FontFamily(Font(identity = "NotoColorEmoji", data = Res.readBytes("files/font/noto-color-emoji-subset.ttf")))
        }
        val loadedCalibration = calibration ?: return@ComposeViewport
        val viewModel = remember(loadedCalibration) {
            EmojiSearchViewModel(
                loadEngine = { onProgress ->
                    // Before the first execution context: contexts built later use the sidecar kernels.
                    installPlatformKernels(onProgress)
                    onProgress("Fetching the LEAF checkpoint (~92 MB)…")
                    val source = LeafSource.Bytes(
                        model = Res.readBytes("files/leaf/${LeafFiles.MODEL}"),
                        denseModel = Res.readBytes("files/leaf/${LeafFiles.DENSE_MODEL}"),
                        configJson = Res.readBytes("files/leaf/${LeafFiles.CONFIG}").decodeToString(),
                        denseConfigJson = Res.readBytes("files/leaf/${LeafFiles.DENSE_CONFIG}").decodeToString(),
                        poolingConfigJson = Res.readBytes("files/leaf/${LeafFiles.POOLING_CONFIG}").decodeToString(),
                        tokenizerJson = Res.readBytes("files/leaf/${LeafFiles.TOKENIZER}").decodeToString(),
                        tokenizerConfigJson = Res.readBytes("files/leaf/${LeafFiles.TOKENIZER_CONFIG}").decodeToString(),
                    )
                    LeafEmbeddingEngine.load(source, onProgress = onProgress)
                },
                loadIndex = { engine, onProgress -> CatalogResources.index(CatalogResources.catalog(), engine, onProgress = onProgress) },
                calibration = loadedCalibration,
                verifierTopK = 0,
            )
        }
        CompositionLocalProvider(LocalEmojiFontFamily provides emojiFont) {
            App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
        }
    }
}
