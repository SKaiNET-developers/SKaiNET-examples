package sk.ainet.samples.emoji

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import semanticemojifilter.composeapp.generated.resources.Res
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.engine.LeafFiles
import sk.ainet.samples.emoji.engine.LeafSource
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel

/**
 * The web build has no filesystem: the checkpoint is bundled as Compose resources (see
 * scripts/fetch-model.sh) and read into memory once. The catalog embeddings are the shipped
 * precomputed file, so the page is interactive right after the encoder is built.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        val viewModel = remember {
            EmojiSearchViewModel(
                loadEngine = { onProgress ->
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
                calibration = Calibration.DEFAULT,
                verifierTopK = 0,
            )
        }
        App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
    }
}
