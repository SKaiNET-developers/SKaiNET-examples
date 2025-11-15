import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {

    androidTarget()


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

            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.lang.models)
            implementation(libs.skainet.compile.core)
            implementation(libs.skainet.backend.cpu)
        }

         wasmJsMain.dependencies {
             implementation(libs.kotlinx.io.core)
         }
    }
}

android {
    namespace = "sk.ai.net.client.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
