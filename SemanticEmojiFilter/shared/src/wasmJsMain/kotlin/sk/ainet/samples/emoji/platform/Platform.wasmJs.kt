package sk.ainet.samples.emoji.platform

import sk.ainet.exec.wasm.WasmKernels
import sk.ainet.exec.wasm.WasmSimd

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.WEB

/** Read after [installPlatformKernels]: the sidecar mode is what the label reports. */
actual fun kernelTierLabel(): String = when {
    !WasmSimd.isInstalled -> "Wasm (scalar)"
    WasmKernels.mode == "simd128-mt" -> "Wasm SIMD128 (threads)"
    else -> "Wasm SIMD128 (single thread)"
}

/**
 * Loads the SIMD128 sidecar of skainet-backend-wasm-simd. The pthreads build is chosen when the
 * page is cross-origin isolated (the coi-serviceworker shim provides that on the examples site);
 * otherwise the single-threaded build. A failure leaves the scalar lane in place.
 */
actual suspend fun installPlatformKernels(onProgress: (String) -> Unit) {
    onProgress("Loading the SIMD128 kernel sidecar…")
    try {
        WasmSimd.install()
    } catch (e: Throwable) {
        logEvent("kernels_sidecar_failed", "reason" to (e.message ?: e::class.simpleName))
    }
    logEvent("kernels", "tier" to kernelTierLabel(), "mode" to WasmKernels.mode)
}

actual fun readTextFile(path: String): String = error("the web build has no filesystem ($path); use LeafSource.Bytes")

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_WASM] $line")
}
