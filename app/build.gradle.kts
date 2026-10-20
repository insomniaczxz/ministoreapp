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

// Check if current build tasks target a release build or version increment
val isBuildingApk = gradle.startParameter.taskNames.any { task ->
    task.contains("assembleRelease", ignoreCase = true) ||
    task.contains("bundleRelease", ignoreCase = true) ||
    task.contains("incrementVersion", ignoreCase = true) ||
    task.equals("assemble", ignoreCase = true) ||
    task.endsWith(":assemble", ignoreCase = true)
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
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

tasks.register("incrementVersion") {
    description = "Increments VERSION_PATCH and VERSION_CODE in version.properties"
    group = "versioning"
    doLast {
        val propFile = file("version.properties")
        if (propFile.canRead()) {
            val props = Properties()
            propFile.inputStream().use { props.load(it) }
            val currentPatch = props["VERSION_PATCH"].toString().toIntOrNull() ?: 0
            val currentCode = props["VERSION_CODE"].toString().toIntOrNull() ?: 0
            props["VERSION_PATCH"] = (currentPatch + 1).toString()
            props["VERSION_CODE"] = (currentCode + 1).toString()
            propFile.outputStream().use { props.store(it, "Updated by incrementVersion task") }
            println("Version updated to: ${props["VERSION_MAJOR"]}.${props["VERSION_MINOR"]}.${props["VERSION_PATCH"]} (Code: ${props["VERSION_CODE"]})")
        }
    }
}
