// The APK shell for :composeApp — no code of its own, only the manifest, the Orbitron font
// asset and the bundled packs. AGP 9 requires the application module to be separate from the
// KMP module (see the library's build script header).
plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "sk.ainet.samples.visiondemo.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sk.ainet.samples.visiondemo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"

        ndk {
            // The ABIs the runner AAR (libskainet_iree_module.so) actually carries.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    packaging {
        // SKaiNET registers kernels through ServiceLoader: merge, never drop, the entries.
        resources { merges += "META-INF/services/**" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Locally materialized packs ride into the APK (assets/packs/<pack_dir>/…) — see
    // bundlePacks below and the licensing section in the README (the packs derive from
    // AGPL-3.0 weights; your build, your obligations — they never enter git, and the app
    // runs without them, showing how to materialize one).
    sourceSets.getByName("main").assets.srcDir("build/bundled-assets")
}

dependencies {
    implementation(projects.composeApp)
}

// Copies materialized packs into the APK assets. Point -PpacksDir= at a directory holding
// pack_dirs (descriptor.json + manifest.json + artifacts/); the default is the blueprint's
// output in a sibling SKaiNET-cartridge-blueprints checkout. Missing directory = no packs
// bundled = the app still builds and runs (pack-less screen).
val packsDir: File = providers.gradleProperty("packsDir").orNull
    ?.let { rootProject.file(it) }
    ?: rootProject.file("../../SKaiNET-cartridge-blueprints/blueprints/detection-yolov8-iree/build/cartridge")

val bundlePacks = tasks.register<Copy>("bundlePacks") {
    from(packsDir) { include("detection-yolov8-iree-*/**") }
    into(layout.buildDirectory.dir("bundled-assets/packs"))
}
tasks.named("preBuild") { dependsOn(bundlePacks) }
