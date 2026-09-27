import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Optional publisher-owned signing material. Public builds need no local configuration.
val signingFile = rootProject.file("local/keystore.properties")
val releaseKey = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }
if (signingFile.exists()) {
    require(listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !releaseKey.getProperty(it).isNullOrBlank() }) {
        "local/keystore.properties is incomplete"
    }
}

android {
    namespace = "dev.liamchu.glance"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.liamchu.glance"
        // DESIGN.md "The stack": 26 is where a notification can carry its own lifetime.
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "2.0"
    }

    signingConfigs {
        if (signingFile.exists()) create("localRelease") {
            storeFile = rootProject.file(releaseKey.getProperty("storeFile"))
            storePassword = releaseKey.getProperty("storePassword")
            keyAlias = releaseKey.getProperty("keyAlias")
            keyPassword = releaseKey.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("localRelease")
            // Without this the app is ~12 MB of Compose and androidx that it never
            // calls. R8 keeps only what is reachable.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        // Backend.kt builds its User-Agent from versionName rather than repeating it (C2).
        buildConfig = true
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

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-installations")
    // Bundled, on-device QR decoding; scanning needs no network or Play services.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    testImplementation("junit:junit:4.13.2")
    // android.jar's org.json is a stub that throws in unit tests; this is the real one.
    testImplementation("org.json:json:20260814")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
