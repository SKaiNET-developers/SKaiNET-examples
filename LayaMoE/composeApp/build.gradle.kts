import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The engine version the header shows, taken from the version catalog at build time so a
// dependency bump can never leave a stale number in the UI (every target reads this constant).
val generateBuildInfo by tasks.registering {
    val skainetVersion = versionCatalogs.named("libs").findVersion("skainet").get().requiredVersion
    val transformersVersion = versionCatalogs.named("libs").findVersion("skainet-transformers").get().requiredVersion
    val outDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("skainetVersion", skainetVersion)
    inputs.property("transformersVersion", transformersVersion)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("sk/ainet/samples/layamoe/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package sk.ainet.samples.layamoe
            |
            |/** Generated from gradle/libs.versions.toml; see generateBuildInfo in composeApp/build.gradle.kts. */
            |object BuildInfo {
            |    const val SKAINET_VERSION: String = "$skainetVersion"
            |    const val TRANSFORMERS_VERSION: String = "$transformersVersion"
            |}
            |""".trimMargin(),
        )
    }
}

// The web build ships the :webWorker: the raw Kotlin/Wasm compiler output (an ES module plus its
// .wasm), copied under moe-worker/ into the wasm resources so the distribution and the dev server
// serve it next to the app.
val copyMoeWorker = tasks.register<Copy>("copyMoeWorker") {
    val worker = project(":webWorker")
    dependsOn(worker.tasks.named("wasmJsProductionExecutableCompileSync"))
    from(worker.layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/kotlin")) {
        exclude("*.map")
        into("moe-worker")
    }
    into(layout.buildDirectory.dir("generated/moeWorker"))
}

kotlin {
    jvmToolchain(21)

    // The Android app entry point (MainActivity, manifest) lives in :androidApp — AGP 9 no
    // longer allows 'com.android.application' in a KMP module. This is a plain KMP library on
    // the Android axis; :androidApp depends on it for App().
    android {
        namespace = "sk.ainet.samples.layamoe.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildInfo)
        }
        commonMain.dependencies {
            implementation(projects.shared)
            implementation("sk.ainet.ui:skainet-ui")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        wasmJsMain {
            dependencies {
                implementation(libs.kotlinx.coroutines)
            }
            resources.srcDir(copyMoeWorker)
        }
    }
}

// Single source of truth for the JVM flags SKaiNET needs on desktop: the JDK Vector API
// (incubator) for SIMD-accelerated CPU ops; the native FFM kernels need no flag.
val skainetSimdJvmArgs = listOf(
    "--add-modules", "jdk.incubator.vector",
    "--enable-preview",
    "-Dskainet.cpu.vector.enabled=true",
)

tasks.withType<JavaExec>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
    // The base model up-casts to 1.6 GB of FP32 weights; Q8_0 (-Dlayamoe.lane=q8) needs less.
    maxHeapSize = "8g"
}

tasks.withType<Test>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
}

compose.desktop {
    application {
        mainClass = "sk.ainet.samples.layamoe.MainKt"

        jvmArgs += skainetSimdJvmArgs
        jvmArgs += "-Xmx8g"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.samples.layamoe"
            packageVersion = "1.0.0"
        }
    }
}
