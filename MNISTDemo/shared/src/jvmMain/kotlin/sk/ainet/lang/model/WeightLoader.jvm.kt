package sk.ainet.lang.model

import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * JVM implementation using reflection-based GGUF loader.
 * This correctly loads PyTorch-exported GGUF weights.
 */
actual fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray) {
    loadGgufWeights(module, bytes)
}
