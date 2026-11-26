package sk.ainet.apps.kllama.chat.playground

import sk.ainet.apps.kllama.chat.qwen.QwenModelSource
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32

// The bundled model arrives as a file (see BundledModel.android.kt), so the
// loader memory-maps the packed weights instead of holding them on the ART heap.
internal actual suspend fun buildQwenRuntimePacked(
    ctx: ExecutionContext,
    source: QwenModelSource,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> = buildQwenRuntimePackedFromSource(ctx, source, bosTokenId)
