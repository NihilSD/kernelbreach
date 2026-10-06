plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure-Kotlin spaced-repetition engine. Deterministic and fully unit-tested on
// the JVM with an injectable clock. Uses kotlinx-datetime (KMP-friendly) so the
// engine stays free of Android types.
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
    api(libs.kotlinx.datetime)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
