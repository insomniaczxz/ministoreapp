import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val versionPropsFile = file("version.properties")
val versionProps = Properties()

if (versionPropsFile.canRead()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
} else {
    versionProps["VERSION_MAJOR"] = "1"
    versionProps["VERSION_MINOR"] = "2"
    versionProps["VERSION_PATCH"] = "4"
    versionProps["VERSION_CODE"] = "4"
}

// Only increment when actually building a Release APK for GitHub (not during Debug/Run/Sync)
val isBuildingApk = gradle.startParameter.taskNames.any { task ->
    task.contains("assembleRelease", ignoreCase = true) ||
    task.contains("bundleRelease", ignoreCase = true)
}

var vMajor = versionProps["VERSION_MAJOR"].toString().toInt()
var vMinor = versionProps["VERSION_MINOR"].toString().toInt()
var vPatch = versionProps["VERSION_PATCH"].toString().toInt()
var vCode = versionProps["VERSION_CODE"].toString().toInt()

if (isBuildingApk) {
    vPatch += 1
    vCode += 1
    versionProps["VERSION_PATCH"] = vPatch.toString()
    versionProps["VERSION_CODE"] = vCode.toString()
    versionPropsFile.outputStream().use { versionProps.store(it, null) }
}

val autoVersionName = "$vMajor.$vMinor.$vPatch"

android {
    namespace = "com.example.ministoreapp"
    compileSdk = 35

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.example.ministoreapp"
        minSdk = 24
        targetSdk = 35
        versionCode = vCode
        versionName = autoVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Gson & Google Auth
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
