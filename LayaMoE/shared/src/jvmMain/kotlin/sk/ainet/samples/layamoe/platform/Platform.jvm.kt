package sk.ainet.samples.layamoe.platform

import java.io.File
import sk.ainet.exec.kernel.PanamaVectorKernelProvider

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.DESKTOP

/** Static capability probe, so the label is right before the kernel registry is populated. */
actual fun kernelTierLabel(): String =
    if (PanamaVectorKernelProvider.isAvailable()) "JVM native FFM + Panama Vector" else "JVM (scalar)"

/** The native kernel provider and packs register themselves on the first dispatch (ServiceLoader). */
actual suspend fun installPlatformKernels(onProgress: (String) -> Unit) = Unit

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_JVM] $line")
}

/** `LAYAMOE_MODEL_DIR`, else `~/.skainet-examples/layamoe` — the desktop's checkpoint directory. */
fun desktopModelDir(): File =
    System.getenv("LAYAMOE_MODEL_DIR")?.takeIf { it.isNotBlank() }?.let(::File)
        ?: File(System.getProperty("user.home"), ".skainet-examples/layamoe")
