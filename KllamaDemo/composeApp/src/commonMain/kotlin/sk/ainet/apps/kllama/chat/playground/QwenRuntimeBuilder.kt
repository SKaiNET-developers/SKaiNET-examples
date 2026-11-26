package sk.ainet.apps.kllama.chat.playground

import sk.ainet.apps.kllama.chat.qwen.ggufBosTokenId
import sk.ainet.apps.kllama.chat.qwen.ggufLoadsPacked
import sk.ainet.apps.kllama.chat.qwen.loadQwenDequantizedToFp32
import sk.ainet.apps.kllama.chat.qwen.loadQwenPacked
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32

/**
 * Builds the Qwen runtime from the bundled GGUF bytes.
 *
 * Packed weights are the default on every platform: quantized tensors stay
 * in their stored block encoding and the matmuls run on the packed bytes.
 * Dequantizing to FP32 is a compatibility escape hatch, taken only when the
 * GGUF contains a tensor type the packed loader does not serve.
 */
internal suspend fun buildQwenRuntime(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> {
    // Qwen's tokenizer reports no BOS (-1), which would crash generation.
    val bos = bosTokenId.takeIf { it >= 0 } ?: ggufBosTokenId(bytes)
    return if (ggufLoadsPacked(bytes)) {
        buildQwenRuntimePacked(ctx, bytes, bos)
    } else {
        OptimizedLLMRuntime(
            model = loadQwenDequantizedToFp32(ctx, bytes),
            ctx = ctx,
            mode = OptimizedLLMMode.DIRECT,
            dtype = FP32::class,
            bos = bos,
        )
    }
}

/**
 * Packed-weights build. The platforms differ only in where the bytes live:
 * JVM stages them to a temp file so the weights are memory-mapped; Android,
 * iOS and web read them in place via [buildQwenRuntimePackedInMemory].
 */
internal expect suspend fun buildQwenRuntimePacked(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32>

internal suspend fun buildQwenRuntimePackedInMemory(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> = OptimizedLLMRuntime(
    model = loadQwenPacked(ctx, bytes),
    ctx = ctx,
    mode = OptimizedLLMMode.DIRECT,
    dtype = FP32::class,
    bos = bosTokenId,
)
