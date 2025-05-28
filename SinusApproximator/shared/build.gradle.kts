import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {

    androidTarget()


    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()


    // Temporarily disable wasmJs target to fix build issues
     @OptIn(ExperimentalWasmDsl::class)
     wasmJs {
         browser {
             val rootDirPath = project.rootDir.path
             val projectDirPath = project.projectDir.path
             commonWebpackConfig {
                 devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                     static = (static ?: mutableListOf()).apply {
                         // Serve sources to debug inside browser
                         add(rootDirPath)
                         add(projectDirPath)
                     }
                 }
             }
         }
     }

    sourceSets {
        // Temporarily disable wasmJsMain source set
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(libs.skainet.core)
            implementation(libs.skainet.io)
            implementation(libs.kotlinx.io.core)
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
