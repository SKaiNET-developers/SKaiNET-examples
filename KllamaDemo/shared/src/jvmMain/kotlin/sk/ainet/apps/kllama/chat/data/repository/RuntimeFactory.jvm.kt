package sk.ainet.apps.kllama.chat.data.repository

import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.ExecutionContext
import sk.ainet.io.RandomAccessSource
import sk.ainet.lang.nn.dsl.decoder.DecoderGgufWeights
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.LlamaNetworkLoader

actual fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    weights: DecoderGgufWeights<FP32, Float>,
    tokenizerSource: () -> RandomAccessSource?
): RuntimeCreationResult {
    // The llama network built from the already loaded tensors, run by the generic decoder
    // runtime: the same chain the Qwen playground uses, no per-architecture runtime class.
    val runtime = OptimizedLLMRuntime(
        model = LlamaNetworkLoader.fromWeights(weights),
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = weights.metadata.bosTokenId.takeIf { it >= 0 } ?: 1,
    )
    // The engine-backed tokenizer reads the vocabulary and merges from the GGUF header; it
    // is what the KernelRace sample and the model providers use, and its ids match the reference.
    val tokenizer = tokenizerSource()?.use { TokenizerFactory.fromGgufSource(it) }
    return RuntimeCreationResult(runtime = runtime, tokenizer = tokenizer)
}
