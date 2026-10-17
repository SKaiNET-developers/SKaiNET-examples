import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ai.net.client.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()


    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        // Temporarily disable wasmJsMain source set
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.lang.models)
            implementation(libs.skainet.compile.core)
            implementation(libs.skainet.compile.dag)
            implementation(libs.skainet.backend.cpu)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }

         wasmJsMain.dependencies {
             implementation(libs.kotlinx.io.core)
         }
    }
}

// The engine version the UI shows ("Powered by SKaiNET …"), taken from the version catalog at
// build time so a dependency bump can never leave a stale number behind.
val generateBuildInfo by tasks.registering {
    val skainetVersion = versionCatalogs.named("libs").findVersion("skainet").get().requiredVersion
    val outDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("skainetVersion", skainetVersion)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("sk/ainet/app/samples/sinus/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package sk.ainet.app.samples.sinus
            |
            |/** Generated from `skainet` in gradle/libs.versions.toml by generateBuildInfo; do not edit. */
            |object BuildInfo {
            |    const val SKAINET_VERSION: String = "$skainetVersion"
            |}
            |""".trimMargin(),
        )
    }
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateBuildInfo)
