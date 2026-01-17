plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.io.core)

    // JVM-optimized SKaiNET backend for inference
    implementation(libs.skainet.backend.cpu.jvm)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

    // SKaiNET dependencies for tests that need direct access to Module
    testImplementation(libs.skainet.lang.core)
    testImplementation(libs.skainet.io.gguf)
}

application {
    mainClass.set("sk.ainet.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.withType<Test> {
    maxHeapSize = "4g"
}
