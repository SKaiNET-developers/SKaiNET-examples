package sk.ainet.apps.kllama.chat.playground

import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32

internal actual suspend fun buildQwenRuntimePacked(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> = buildQwenRuntimePackedInMemory(ctx, bytes, bosTokenId)
