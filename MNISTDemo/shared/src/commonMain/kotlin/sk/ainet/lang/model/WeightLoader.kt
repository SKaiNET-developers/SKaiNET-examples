package sk.ainet.lang.model

import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

/**
 * Platform-specific weight loader for GGUF files.
 * On JVM, uses reflection-based loader that correctly handles PyTorch-exported GGUF.
 * On other platforms, falls back to library loader.
 */
expect fun loadWeightsFromBytes(module: Module<FP32, Float>, bytes: ByteArray)
