package sk.ainet.apps.kllama.chat.playground

import sk.ainet.exec.wasm.WasmSimd

internal actual suspend fun installPlatformKernels() {
    try {
        WasmSimd.install()
    } catch (e: Throwable) {
        // The scalar kernels stay in place, so the app still works, only slower.
        println("[KllamaDemo] SIMD128 sidecar unavailable: ${e.message ?: e::class.simpleName}")
    }
}
