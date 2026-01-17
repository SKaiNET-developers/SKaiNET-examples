package sk.ainet.lang.model

import kotlinx.io.Buffer
import kotlinx.io.Source
import sk.ainet.lang.model.loader.loadModelWeights
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * WasmJS implementation using library loader.
 * Note: This may not work correctly with PyTorch-exported GGUF files.
 */
actual fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray) {
    val source: Source = Buffer().apply { write(bytes) }
    loadModelWeights(module, source)
}
