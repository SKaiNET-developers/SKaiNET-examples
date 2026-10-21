package sk.ainet.samples.layamoe.platform

import android.util.Log
import sk.ainet.exec.kernel.jni.JniKernelProvider

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.ANDROID

actual fun kernelTierLabel(): String =
    if (JniKernelProvider.isAvailable()) "ARM NEON (JNI)" else "NEON (unavailable)"

actual suspend fun installPlatformKernels(onProgress: (String) -> Unit) = Unit

internal actual fun platformLog(line: String) {
    // Host unit tests run against stubbed android.jar, where Log throws "not mocked".
    try {
        Log.i("SKAINET_PERF", line)
    } catch (_: RuntimeException) {
        println("[SKAINET_PERF] $line")
    }
}
