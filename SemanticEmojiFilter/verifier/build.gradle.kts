import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Laya decision model as a second-stage verifier. Desktop and Android only: the checkpoint
// is 644 MB of F16 weights (about 1.3 GB as FP32 in memory), far outside the web budget.
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

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            implementation(libs.kotlinx.coroutines)
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
