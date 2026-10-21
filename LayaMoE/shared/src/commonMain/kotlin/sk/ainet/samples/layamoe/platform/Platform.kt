package sk.ainet.samples.layamoe.platform

import kotlin.time.TimeSource

/** Every target this sample runs on — the fixed set the platform-chips row displays. */
enum class SamplePlatform(val label: String) {
    ANDROID("Android"),
    DESKTOP("Desktop"),
    WEB("Web"),
}

/** Which of [SamplePlatform] this process is actually running as. */
expect val currentSamplePlatform: SamplePlatform

/** Human-readable label for the kernel path this platform/process is running — shown in the UI. */
expect fun kernelTierLabel(): String

/**
 * Brings up the fastest kernel tier of this platform; call it once, before the first execution
 * context is built. Desktop (native FFM + Panama Vector) and Android (JNI NEON) pick their tier
 * at startup, so their actuals are no-ops; the web actual loads the SIMD128 sidecar and falls
 * back to the scalar lane when that fails, so the page always comes up.
 */
expect suspend fun installPlatformKernels(onProgress: (String) -> Unit)

/** Where the platform writes a log line — `adb logcat` on Android, stdout elsewhere. */
internal expect fun platformLog(line: String)

private val processStart = TimeSource.Monotonic.markNow()

/** One-line structured events: `event=... | +123ms | key=value`, greppable on every platform. */
fun logEvent(name: String, vararg details: Pair<String, Any?>) {
    val line = buildString {
        append("event=").append(name)
        append(" | +").append(processStart.elapsedNow().inWholeMilliseconds).append("ms")
        for ((key, value) in details) {
            if (value != null) append(" | ").append(key).append('=').append(value)
        }
    }
    platformLog(line)
}
