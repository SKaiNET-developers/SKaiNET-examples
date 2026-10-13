package sk.ainet.samples.emoji.platform

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.WEB

actual fun kernelTierLabel(): String = "Wasm (scalar)"

actual fun readTextFile(path: String): String = error("the web build has no filesystem ($path); use LeafSource.Bytes")

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_WASM] $line")
}
