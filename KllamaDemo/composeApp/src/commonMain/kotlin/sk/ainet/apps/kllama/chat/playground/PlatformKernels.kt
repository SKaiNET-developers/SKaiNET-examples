package sk.ainet.apps.kllama.chat.playground

/**
 * Makes the platform's native matmul kernels available for packed weights.
 * Call once, before the first execution context is created. Without it the
 * packed matmuls run on the scalar reference kernels, which is far slower.
 *
 * - JVM and Android: nothing to do, the backend on the classpath registers
 *   itself (`skainet-backend-native-cpu` / `skainet-backend-jni-cpu`).
 * - iOS: registers the NEON kernels from `skainet-backend-native-cpu`.
 * - wasmJs: loads the SIMD128 sidecar from `skainet-backend-wasm-simd`.
 * - JS: no native backend exists, the scalar kernels stay in place.
 */
internal expect suspend fun installPlatformKernels()
