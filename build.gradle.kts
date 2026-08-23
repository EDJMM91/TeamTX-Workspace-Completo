// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.download) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}

allprojects {
    group = "com.aistudio.teamtxvzla"
    
    // Set java_shared_conf for OsmAnd-java compatibility
    gradle.extra["java_shared_conf"] = ""
}