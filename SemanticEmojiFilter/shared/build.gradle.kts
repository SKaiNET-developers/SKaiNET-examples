import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.samples.emoji.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Runs commonTest on the JVM against Android stubs (testAndroidHostTest) — CI relies on it.
        withHostTest {}
    }

    jvm()

    // No iOS: the bert, laya and decisions modules of SKaiNET-transformers publish no Apple targets.
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
            // composeApp / cli classpaths, which construct the execution context themselves.
            api(project.dependencies.platform(libs.skainet.bom))
            api(libs.skainet.lang.core)
            api(libs.skainet.backend.cpu)
            api(libs.skainet.io.core)
            api(libs.skainet.io.safetensors)
            api(libs.skainet.frontend.text)

            // SKaiNET-transformers: the BERT encoder runtime (mean pooling + 2_Dense projection).
            api(project.dependencies.platform(libs.skainet.transformers.bom))
            api(libs.skainet.transformers.inference.bert)
        }
        // Desktop and Android share the file-based checkpoint download (both are JVM code).
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // Streamed download of the checkpoint files from the Hugging Face Hub.
                implementation(libs.skainet.data.source)
            }
        }
        jvmMain {
            dependsOn(jvmAndroidMain)
            dependencies {
                // PanamaVectorKernelProvider.isAvailable() for the kernel-tier label.
                implementation(libs.skainet.backend.api)
            }
        }
        androidMain {
            dependsOn(jvmAndroidMain)
            dependencies {
                // Hand-written ARM NEON kernels, dispatched at runtime.
                implementation(libs.skainet.backend.jni.cpu)
            }
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
