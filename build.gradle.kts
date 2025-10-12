// Top-level build.gradle.kts

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.3.15") // Stable version
    }
}

