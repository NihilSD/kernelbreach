// Root build file.
//
// No plugins are applied at the root. Each module declares exactly the plugins
// it needs via the version catalog (gradle/libs.versions.toml). This keeps the
// root project free of the Android Gradle Plugin, so the pure-Kotlin core can
// be configured and tested in environments without an Android SDK (or access to
// Google's Maven repository).
//
// See settings.gradle.kts for how Android modules are conditionally included.
