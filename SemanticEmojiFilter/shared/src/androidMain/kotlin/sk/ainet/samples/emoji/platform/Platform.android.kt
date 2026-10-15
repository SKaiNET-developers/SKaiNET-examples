package sk.ainet.samples.emoji.platform

import android.util.Log
import java.io.File
import sk.ainet.exec.kernel.jni.JniKernelProvider

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.ANDROID

actual fun kernelTierLabel(): String =
    if (JniKernelProvider.isAvailable()) "ARM NEON" else "NEON (unavailable)"

actual fun readTextFile(path: String): String = File(path).readText()

internal actual fun platformLog(line: String) {
    // Host unit tests run against stubbed android.jar, where Log throws "not mocked".
    try {
        Log.i("SKAINET_PERF", line)
    } catch (_: RuntimeException) {
        println("[SKAINET_PERF] $line")
    }
}
