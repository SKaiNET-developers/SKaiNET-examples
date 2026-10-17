import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Laya decision model as a second-stage verifier. Desktop and Android load the 644 MB F16
// checkpoint from files; the web build (wasmJs, inside the :webVerifier worker) fetches it from
// Hugging Face into the browser cache and quantizes it to Q8_0 at load (about 340 MB in memory).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.samples.emoji.verifier"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            implementation(libs.kotlinx.coroutines)
            // The BOMs again (shared applies them too): the wasm npm-aggregation configuration
            // resolves this module's own dependencies without the constraints a project
            // dependency carries, and would see them versionless.
            implementation(project.dependencies.platform(libs.skainet.bom))
            implementation(project.dependencies.platform(libs.skainet.transformers.bom))
            api(libs.skainet.transformers.decisions.api)
            implementation(libs.skainet.transformers.decisions.runtime)
            implementation(libs.skainet.transformers.inference.laya)
        }
        // Desktop and Android share the file-based checkpoint source (both are JVM code).
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlinx.io.core)
                implementation(libs.skainet.data.source)
            }
        }
        jvmMain { dependsOn(jvmAndroidMain) }
        androidMain { dependsOn(jvmAndroidMain) }
        wasmJsMain.dependencies {
            // The SIMD128 sidecar, installed inside the worker before the first execution context.
            implementation(libs.skainet.backend.wasm.simd)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-modules", "jdk.incubator.vector", "--enable-preview", "-Dskainet.cpu.vector.enabled=true")
    maxHeapSize = "6g"
}
