plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.liamchu.glance"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.liamchu.glance"
        // DESIGN.md "The stack": 26 is where a notification can carry its own lifetime.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    testImplementation("junit:junit:4.13.2")
    // android.jar's org.json is a stub that throws in unit tests; this is the real one.
    testImplementation("org.json:json:20260814")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
