import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The mixture as a Web Worker: a Kotlin/Wasm executable whose raw compiler output the web app
// ships under moe-worker/ and starts as a module worker (see composeApp/build.gradle.kts).
// Everything the worker does (download, quantize-at-load, routing, decisions) runs off the
// page's thread.
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
            implementation(libs.kotlinx.coroutines)
            // The BOMs again: the wasm npm-aggregation configuration resolves this module's own
            // dependencies without the constraints a project dependency carries.
            implementation(project.dependencies.platform(libs.skainet.bom))
            implementation(project.dependencies.platform(libs.skainet.transformers.bom))
        }
    }
}
