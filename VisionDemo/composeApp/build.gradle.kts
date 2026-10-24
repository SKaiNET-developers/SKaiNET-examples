import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// All UI lives in commonMain against the published KMP cartridge API (YoloEngine, Detection);
// the camera feed, pack discovery and engine opening are expect/actual, with Android
// (CameraX + the IREE module runner) as the first wired platform — a desktop or iOS target
// needs only its own actuals. AGP 9 does not allow a KMP module to be the application
// itself, so :androidApp is the (code-free) APK shell.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.samples.visiondemo"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines)
            implementation("sk.ainet.ui:skainet-ui")
            implementation(libs.cartridge.detection.yolov8)
        }
        androidMain.dependencies {
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
        }
    }
}

// The versions the UI shows ("Powered by SKaiNET …", pack-less screen), taken from the
// version catalog at build time so a dependency bump can never leave a stale number behind.
val generateBuildInfo by tasks.registering {
    val skainetVersion = versionCatalogs.named("libs").findVersion("skainet").get().requiredVersion
    val cartridgeVersion = versionCatalogs.named("libs").findVersion("cartridge").get().requiredVersion
    val outDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("skainetVersion", skainetVersion)
    inputs.property("cartridgeVersion", cartridgeVersion)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("sk/ainet/samples/visiondemo/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package sk.ainet.samples.visiondemo
            |
            |/** Generated from gradle/libs.versions.toml by generateBuildInfo; do not edit. */
            |const val SKAINET_VERSION: String = "$skainetVersion"
            |const val CARTRIDGE_VERSION: String = "$cartridgeVersion"
            |""".trimMargin(),
        )
    }
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateBuildInfo)
