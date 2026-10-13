package sk.ainet.samples.emoji.verifier

import java.io.File
import java.nio.file.Path
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32
import sk.ainet.models.laya.LayaAgent
import sk.ainet.models.laya.LayaModelLoader

actual fun loadLayaAgent(directory: String, ctx: ExecutionContext): LayaAgent<FP32> =
    LayaModelLoader.load(Path.of(directory), ctx, exactGelu = true)

/** Desktop: the checkpoint lands next to the LEAF cache, or where `LAYA_MODEL_DIR` points. */
fun layaDesktopSource(): DecisionAdapterSource = LayaFileSource(
    File(
        System.getenv("LAYA_MODEL_DIR")?.takeIf { it.isNotBlank() }
            ?: File(System.getProperty("user.home"), ".skainet-examples/semantic-emoji-filter/laya-multilingual").path,
    ),
)
