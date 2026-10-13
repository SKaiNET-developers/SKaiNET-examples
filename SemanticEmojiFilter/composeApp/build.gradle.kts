import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val layaVerifier = providers.gradleProperty("layaVerifier").orNull == "true"

kotlin {
    jvmToolchain(21)

    // The Android app entry point (MainActivity, manifest) lives in :androidApp — AGP 9 no
    // longer allows 'com.android.application' in a KMP module. This is a plain KMP library on
    // the Android axis; :androidApp depends on it for App().
    android {
        namespace = "sk.ainet.samples.emoji.library"
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
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.coroutines)
        }
        // The Laya verifier is a build-time switch: with -PlayaVerifier=true the :verifier module
        // is on the Desktop and Android classpath and the *Verifier source dirs provide
        // createVerifier(); without it the *NoVerifier dirs return the no-op verifier.
        val verifierDir = if (layaVerifier) "Verifier" else "NoVerifier"
        jvmMain {
            kotlin.srcDir("src/jvm$verifierDir/kotlin")
            if (layaVerifier) dependencies { implementation(project(":verifier")) }
        }
        androidMain {
            kotlin.srcDir("src/android$verifierDir/kotlin")
            if (layaVerifier) dependencies { implementation(project(":verifier")) }
        }
    }
}

// Single source of truth for the JVM flags SKaiNET needs on desktop: the JDK Vector API
// (incubator) for SIMD-accelerated CPU ops.
val skainetSimdJvmArgs = listOf(
    "--add-modules", "jdk.incubator.vector",
    "--enable-preview",
    "-Dskainet.cpu.vector.enabled=true",
)

tasks.withType<JavaExec>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
    maxHeapSize = if (layaVerifier) "6g" else "2g"
}

tasks.withType<Test>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
}

// The web build bundles the LEAF checkpoint as Compose resources (wasm has no filesystem).
// Desktop and Android download the same files at runtime instead, so the directory sits under
// wasmJsMain and never reaches the jar or the APK.
val leafDir = layout.projectDirectory.dir("src/wasmJsMain/composeResources/files/leaf")

val fetchModel by tasks.registering(Exec::class) {
    description = "Downloads the MongoDB LEAF (mdbr-leaf-ir) checkpoint into the wasm Compose resources. Skips if present."
    group = "build setup"
    val scriptPath = rootProject.layout.projectDirectory.file("scripts/fetch-model.sh")
    commandLine("bash", scriptPath.asFile.absolutePath, leafDir.asFile.absolutePath)
    inputs.file(scriptPath)
    outputs.dir(leafDir)
    val modelFile = leafDir.file("model.safetensors").asFile
    onlyIf { !modelFile.exists() || modelFile.length() < 80L * 1024 * 1024 }
}

// Every Kotlin compile task AND every Compose-resources copy task depends on the model being on
// disk — Gradle 9's strict implicit-dependency validator fails otherwise, since the
// resource-copy task reads from a directory fetchModel writes into.
tasks.matching {
    it.name.startsWith("compileKotlin") ||
        it.name.startsWith("compileJava") ||
        it.name.startsWith("convertXmlValueResourcesFor") ||
        it.name.startsWith("copyNonXmlValueResourcesFor") ||
        it.name.startsWith("prepareComposeResourcesTaskFor") ||
        it.name.startsWith("generateResourceAccessorsFor")
}.configureEach { dependsOn(fetchModel) }

compose.desktop {
    application {
        mainClass = "sk.ainet.samples.emoji.MainKt"

        jvmArgs += skainetSimdJvmArgs
        jvmArgs += if (layaVerifier) "-Xmx6g" else "-Xmx2g"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.samples.emoji"
            packageVersion = "1.0.0"
        }
    }
}
