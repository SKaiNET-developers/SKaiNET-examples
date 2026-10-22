package sk.ainet.apps.kllama.chat.data.repository

import sk.ainet.context.ExecutionContext
import sk.ainet.io.RandomAccessSource
import sk.ainet.lang.nn.dsl.decoder.DecoderGgufWeights
import sk.ainet.lang.types.FP32

actual fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    weights: DecoderGgufWeights<FP32, Float>,
    tokenizerSource: () -> RandomAccessSource?
): RuntimeCreationResult = RuntimeCreationResult(runtime = null, tokenizer = null)
