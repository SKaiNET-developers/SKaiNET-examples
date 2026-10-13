package sk.ainet.samples.emoji.platform

import java.io.File
import sk.ainet.exec.kernel.PanamaVectorKernelProvider

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.DESKTOP

/** Static capability probe, so the label is right before the kernel registry is populated. */
actual fun kernelTierLabel(): String =
    if (PanamaVectorKernelProvider.isAvailable()) "Panama Vector (SIMD)" else "JVM (scalar)"

actual fun readTextFile(path: String): String = File(path).readText()

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_JVM] $line")
}
