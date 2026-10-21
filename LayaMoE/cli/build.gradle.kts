plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    // The native FFM kernels on the JVM (also pulled in by :shared's jvm target; listed for clarity).
    implementation(libs.skainet.backend.native.cpu)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("sk.ainet.samples.layamoe.cli.MainKt")
    // The installed launcher (installDist) gets the same flags as :cli:run: the Vector API for the
    // Panama lane, native access for the FFM kernels, and room for the FP32 weights.
    applicationDefaultJvmArgs = listOf(
        "--enable-preview", "--add-modules", "jdk.incubator.vector", "--enable-native-access=ALL-UNNAMED",
        "-Dskainet.cpu.vector.enabled=true", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Xmx8g",
    )
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    systemProperty("skainet.cpu.vector.enabled", "true")
    maxHeapSize = "8g"
    standardInput = System.`in`
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
}

// The lifecycle-viewmodel artifact reaches the runtime classpath twice (its jvm and desktop variants); the distribution keeps one.
tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
