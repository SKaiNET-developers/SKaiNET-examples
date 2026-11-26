package sk.ainet.apps.kllama.chat.playground

import java.nio.file.Files
import java.nio.file.Path
import sk.ainet.apps.kllama.chat.qwen.QwenModelSource
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.io.JvmRandomAccessSource
import sk.ainet.lang.types.FP32
import sk.ainet.lang.nn.dsl.decoder.DecoderGgufWeightLoader
import sk.ainet.models.qwen.QwenNetworkLoader

private val QWEN_ARCHS = setOf("qwen2", "qwen3", "qwen35")

internal actual suspend fun buildQwenRuntimePacked(
    ctx: ExecutionContext,
    source: QwenModelSource,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> {
    // Memory-mapping needs a file. The bundled model arrives as an in-memory
    // ByteArray from Compose Resources, so it is staged to a temp file that
    // is reused across the loader's metadata + tensor passes and deleted on
    // JVM exit.
    val modelPath: String = when (source) {
        is QwenModelSource.InMemory -> stageBytesToTempFile(source.bytes).toString()
        is QwenModelSource.OnDisk -> source.path
    }

    // The default WeightForm (null) keeps quantized tensors packed in their stored
    // block encoding, memory-mapped; the engine's row-major kernel packs serve the
    // matmuls on those bytes. Token embeddings stay packed too and are
    // dequantized row by row on gather.
    val loader = DecoderGgufWeightLoader(
        randomAccessProvider = { JvmRandomAccessSource.open(modelPath) },
        acceptedArchitectures = QWEN_ARCHS,
    )

    val weights = loader.loadToMapStreaming<FP32, Float>(ctx)
    val model = QwenNetworkLoader.fromWeights(weights)
    return OptimizedLLMRuntime(
        model = model,
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = weights.metadata.bosTokenId.takeIf { it >= 0 } ?: bosTokenId,
    )
}

private fun stageBytesToTempFile(bytes: ByteArray): Path {
    // createTempFile already creates an empty file; Files.write will
    // overwrite it. deleteOnExit removes it at JVM shutdown so we don't
    // accumulate 380 MB temp files across launches.
    val tempFile = Files.createTempFile("kllamademo-qwen-", ".gguf")
    Files.write(tempFile, bytes)
    tempFile.toFile().deleteOnExit()
    return tempFile
}
