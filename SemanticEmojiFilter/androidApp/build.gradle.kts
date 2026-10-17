plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val layaVerifier = providers.gradleProperty("layaVerifier").orNull == "true"

kotlin {
    dependencies {
        implementation(projects.composeApp)
        implementation(projects.shared)
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(libs.compose.uiTooling)
        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlinx.coroutines)
        // Hand-written ARM NEON kernels — Android JNI only.
        implementation(libs.skainet.backend.jni.cpu)
    }

    target {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

android {
    namespace = "sk.ainet.samples.emoji"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sk.ainet.samples.emoji"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // ARM only — the JNI NEON kernels are what makes the encoder fast on a phone.
        ndk { abiFilters += "arm64-v8a" }

        buildConfigField("boolean", "VERIFIER_ENABLED", layaVerifier.toString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
