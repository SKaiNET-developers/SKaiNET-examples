import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The Laya verifier as a Web Worker: a Kotlin/Wasm executable whose raw compiler output the web
// app ships under laya-worker/ and starts as a module worker (see composeApp/build.gradle.kts).
// Everything the worker does (download, quantize-at-load, decisions) runs off the page's thread.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.shared)
            implementation(projects.verifier)
            implementation(libs.kotlinx.coroutines)
        }
    }
}
