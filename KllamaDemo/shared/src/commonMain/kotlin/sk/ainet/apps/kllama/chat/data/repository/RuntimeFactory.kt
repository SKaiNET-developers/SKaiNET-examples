package sk.ainet.apps.kllama.chat.data.repository

import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.context.ExecutionContext
import sk.ainet.io.RandomAccessSource
import sk.ainet.lang.nn.dsl.decoder.DecoderGgufWeights
import sk.ainet.lang.types.FP32

/**
 * The runtime and tokenizer built from one loaded GGUF.
 *
 * @property runtime the decoder runtime over the llama network (JVM); null where this sample
 *   runs no chat inference yet
 * @property tokenizer the GGUF's own tokenizer through the engine text front-end (JVM); null likewise
 */
data class RuntimeCreationResult(
    val runtime: OptimizedLLMRuntime<FP32>?,
    val tokenizer: Tokenizer?
)

/**
 * Platform-specific factory for the chat runtime and tokenizer.
 *
 * On JVM: `OptimizedLLMRuntime` over `LlamaNetworkLoader.fromWeights(weights)` and
 * `TokenizerFactory.fromGgufSource`, both from SKaiNET-transformers' common API.
 * On other platforms: nulls, so the chat stays in demo mode (the Qwen playground has its own loader).
 *
 * @param weights the GGUF tensors and metadata as the decoder loader returned them
 * @param tokenizerSource opens the GGUF for the tokenizer's random-access read of the header;
 *   returns null where the platform cannot
 */
expect fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    weights: DecoderGgufWeights<FP32, Float>,
    tokenizerSource: () -> RandomAccessSource?
): RuntimeCreationResult
