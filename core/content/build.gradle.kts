plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Pure-Kotlin content layer: JSON schema, parser, validator and the assembler
// that turns the bundled content into the domain [Curriculum]. No Android types,
// so the whole import pipeline is unit-tested on the JVM.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
    // The validator test loads the real bundled content from the repo root.
    systemProperty("kb.contentDir", rootProject.file("content").absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
