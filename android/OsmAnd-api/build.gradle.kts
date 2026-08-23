import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ivy.publish)
}

android {
    namespace = "net.osmand.aidlapi"
    compileSdk = libs.versions.osmand.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.osmand.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.osmand.minSdk.get().toInt()
        targetSdk = libs.versions.osmand.targetSdk.get().toInt()
        versionCode = 2
        versionName = "2.0"
    }
    lintOptions {
        abortOnError = false
    }
    sourceSets {
        main {
            manifest.srcFile("AndroidManifest.xml")
            aidl.srcDirs("src")
            java.srcDirs("src")
        }
    }
    buildFeatures {
        aidl = true
    }
    publishing {
        singleVariant("release") {
            withJavadocJar()
        }
        singleVariant("debug") {
            withJavadocJar()
        }
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

version = System.getenv("OSMAND_BINARIES_IVY_REVISION") ?: "master-snapshot"

afterEvaluate {
    publishing {
        repositories {
            ivy {
                url = uri(System.getenv("OSMAND_BINARIES_IVY_ROOT") ?: "./")
            }
        }
        publications {
            create<IvyPublication>("release") {
                from(components["release"])
                organisation = "net.osmand"
                module = "android-aidl-lib"
            }
            create<IvyPublication>("debug") {
                from(components["debug"])
                organisation = "net.osmand"
                module = "android-aidl-lib"
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.annotation:annotation:1.7.0")
}

tasks.register("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.main.java.srcDirs)
}

artifacts {
    archives(tasks["sourcesJar"])
}