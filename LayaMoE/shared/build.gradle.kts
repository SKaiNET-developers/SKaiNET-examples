import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Everything the example does — checkpoint spec and download, the mixture of expert heads, the
// presets, the question JSON, the worker protocol, the view model — is common code. The platform
// source sets hold only the three seams: where the model bytes come from, which kernels to
// install, and (web) the Worker bootstrap.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.samples.layamoe.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Runs commonTest on the JVM against Android stubs (testAndroidHostTest) — CI relies on it.
        withHostTest {}
    }

    jvm()

    // No iOS: the laya and decisions modules of SKaiNET-transformers publish no Apple targets.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)

            // SKaiNET core. BOM applied via api() so its version constraints also reach the
            // composeApp / cli / webWorker classpaths.
            api(project.dependencies.platform(libs.skainet.bom))
            api(libs.skainet.lang.core)
            api(libs.skainet.backend.cpu)
            api(libs.skainet.io.core)
            api(libs.skainet.io.safetensors)
            api(libs.skainet.frontend.text)
            // Remote artifacts: the checkpoint files are fetched through the engine's data-source resolver
            // (Ktor CIO on the JVM and Android, the browser's fetch in the worker).
            implementation(libs.skainet.data.source)

            // SKaiNET-transformers: the Laya model, its expert heads and the typed decision contract.
            api(project.dependencies.platform(libs.skainet.transformers.bom))
            api(libs.skainet.transformers.decisions.api)
            api(libs.skainet.transformers.decisions.runtime)
            api(libs.skainet.transformers.inference.laya)
        }
        // Desktop and Android share the file-based checkpoint directory (both are JVM code).
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        jvmMain {
            dependsOn(jvmAndroidMain)
            dependencies {
                // PanamaVectorKernelProvider.isAvailable() for the kernel-tier label.
                implementation(libs.skainet.backend.api)
                // The native FFM kernels: fused attention / norms / softmax and the packed matmuls on the JVM.
                implementation(libs.skainet.backend.native.cpu)
            }
        }
        androidMain {
            dependsOn(jvmAndroidMain)
            dependencies {
                // Hand-written ARM NEON kernels, dispatched at runtime.
                implementation(libs.skainet.backend.jni.cpu)
            }
        }
        wasmJsMain.dependencies {
            // SIMD128 sidecar kernels (WasmSimd.install() before the first execution context).
            implementation(libs.skainet.backend.wasm.simd)
            // org.w3c.files.Blob for the engine's JsBlobRandomAccessSource.
            implementation(libs.kotlinx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test>().configureEach {
    // SIMD-accelerated CPU ops via the JDK Vector API (incubator), same flags as the desktop run.
    jvmArgs(
        "--add-modules", "jdk.incubator.vector",
        "--enable-preview",
        "-Dskainet.cpu.vector.enabled=true",
    )
    maxHeapSize = "4g"
}
