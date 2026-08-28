// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  // Fija la versión del KGP una sola vez; los módulos lo aplican sin versión
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.10" apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.crashlytics) apply false
  // Plugins del motor OsmAnd
  alias(libs.plugins.android.library) apply false
  // Requerido por el motor OsmAnd (apply plugin en ../android/OsmAnd)
  id("de.undercouch.download") version "4.1.1" apply false
}

// Alinea el JVM target de todos los módulos (incluido el motor OsmAnd) con Java 17
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}
