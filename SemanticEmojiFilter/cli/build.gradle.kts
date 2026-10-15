plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

val layaVerifier = providers.gradleProperty("layaVerifier").orNull == "true"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    // Panama FFM kernels for the JVM.
    implementation(libs.skainet.backend.api)
    if (layaVerifier) implementation(project(":verifier"))

    testImplementation(kotlin("test"))
}

// The CLI reads the same catalog files the apps bundle, so one copy stays the source of truth.
// The `verify` command exists only when the verifier module is part of the build.
sourceSets.main {
    resources.srcDir("../composeApp/src/commonMain/composeResources/files")
    kotlin.srcDir(if (layaVerifier) "src/verifier/kotlin" else "src/noVerifier/kotlin")
}

application {
    mainClass.set("sk.ainet.samples.emoji.cli.MainKt")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    systemProperty("skainet.cpu.vector.enabled", "true")
    maxHeapSize = if (layaVerifier) "6g" else "2g"
    standardInput = System.`in`
}
