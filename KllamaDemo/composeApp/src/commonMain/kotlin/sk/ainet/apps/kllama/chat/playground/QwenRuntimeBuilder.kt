package sk.ainet.apps.kllama.chat.playground

import sk.ainet.apps.kllama.chat.qwen.QwenModelSource
import sk.ainet.apps.kllama.chat.qwen.ggufBosTokenId
import sk.ainet.apps.kllama.chat.qwen.ggufLoadsPacked
import sk.ainet.apps.kllama.chat.qwen.loadQwenDequantizedToFp32
import sk.ainet.apps.kllama.chat.qwen.loadQwenPacked
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32

/**
 * Builds the Qwen runtime from the bundled GGUF.
 *
 * Packed weights are the default on every platform: quantized tensors stay
 * in their stored block encoding and the matmuls run on the packed bytes.
 * Dequantizing to FP32 is a compatibility escape hatch, taken only when the
 * GGUF contains a tensor type the packed loader does not serve.
 */
internal suspend fun buildQwenRuntime(
    ctx: ExecutionContext,
    source: QwenModelSource,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> {
    // Qwen's tokenizer reports no BOS (-1), which would crash generation.
    val bos = bosTokenId.takeIf { it >= 0 } ?: ggufBosTokenId(source)
    return if (ggufLoadsPacked(source)) {
        buildQwenRuntimePacked(ctx, source, bos)
    } else {
        OptimizedLLMRuntime(
            model = loadQwenDequantizedToFp32(ctx, source),
            ctx = ctx,
            mode = OptimizedLLMMode.DIRECT,
            dtype = FP32::class,
            bos = bos,
        )
    }
}

/**
 * Packed-weights build. Weights are memory-mapped when the model is a file:
 * always on JVM, which stages in-memory bytes to a temp file, and on Android,
 * where the bundled model is a file. iOS and web read the bytes in place.
 */
internal expect suspend fun buildQwenRuntimePacked(
    ctx: ExecutionContext,
    source: QwenModelSource,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32>

internal suspend fun buildQwenRuntimePackedFromSource(
    ctx: ExecutionContext,
    source: QwenModelSource,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> = OptimizedLLMRuntime(
    model = loadQwenPacked(ctx, source),
    ctx = ctx,
    mode = OptimizedLLMMode.DIRECT,
    dtype = FP32::class,
    bos = bosTokenId,
)
