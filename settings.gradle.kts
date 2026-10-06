pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Restrict Google's Maven to the groups it actually serves, so builds of
        // the pure-Kotlin core never attempt (and fail) to resolve ordinary JVM
        // artifacts from it.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "KernelBreach"

// ---------------------------------------------------------------------------
// Pure-Kotlin core modules. No Android types, no Android SDK required.
// These always build and `./gradlew test` runs their unit tests anywhere a
// JDK + Gradle is present (CI, a laptop, or a headless container).
// ---------------------------------------------------------------------------
include(":core:model")
include(":core:content")
include(":core:srs")

// ---------------------------------------------------------------------------
// Android modules. Only included when an Android SDK is available, so the
// JVM core can be tested in environments without the SDK. A normal Android
// dev machine / CI has ANDROID_HOME set (or a local.properties with sdk.dir),
// so it gets the full app. Force-enable with `-PwithAndroid=true`.
// ---------------------------------------------------------------------------
val androidSdkAvailable: Boolean =
    System.getenv("ANDROID_HOME") != null ||
        System.getenv("ANDROID_SDK_ROOT") != null ||
        file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") } ||
        (providers.gradleProperty("withAndroid").orNull == "true")

if (androidSdkAvailable) {
    include(":app")
    include(":core:design")
    include(":core:database")
    include(":feature:onboarding")
    include(":feature:home")
    include(":feature:map")
    include(":feature:module")
    include(":feature:lesson")
    include(":feature:lab")
    include(":feature:checkpoint")
    include(":feature:refresh")
    include(":feature:library")
    include(":feature:me")
} else {
    gradle.startParameter.let {
        logger.lifecycle(
            "[KernelBreach] No Android SDK detected — configuring JVM core modules only " +
                "(:core:model, :core:content, :core:srs). Set ANDROID_HOME or pass " +
                "-PwithAndroid=true to include the Android app.",
        )
    }
}
