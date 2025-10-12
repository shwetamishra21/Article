plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.23"
}

android {
    namespace = "com.example.article"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.article"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Explicit latest Material3 version that supports outlinedTextFieldColors
    implementation("androidx.compose.material3:material3:1.2.0-beta02")

    // Compose UI
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Material icons
    implementation("androidx.compose.material:material-icons-extended")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
