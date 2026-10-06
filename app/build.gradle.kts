plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.kernelbreach.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kernelbreach.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Placeholder release signing. Real keystore values come from a local,
        // untracked keystore.properties (see README); never commit a keystore.
        create("release") {
            val props = rootProject.file("keystore.properties")
            if (props.exists()) {
                val p = java.util.Properties().apply { load(props.inputStream()) }
                storeFile = file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val props = rootProject.file("keystore.properties")
            if (props.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

// --- Bundle the repo-root content/ into the app's assets (single source) -----
// Keeps one canonical copy at the repo root; the app packages it from a
// generated assets dir so nothing is duplicated in version control.
val contentAssetsDir = layout.buildDirectory.dir("generated/contentAssets")
val syncContentAssets = tasks.register<Sync>("syncContentAssets") {
    description = "Copies repo-root content/ into the app's generated assets."
    from(rootProject.file("content")) { into("content") }
    into(contentAssetsDir)
}
android {
    sourceSets.getByName("main").assets.srcDir(contentAssetsDir)
}
tasks.named("preBuild").configure { dependsOn(syncContentAssets) }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:content"))
    implementation(project(":core:srs"))
    implementation(project(":core:design"))
    implementation(project(":core:database"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.work.runtime.ktx)

    // Screenshot tests (JVM, via Robolectric + Roborazzi)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
