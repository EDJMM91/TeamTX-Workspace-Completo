pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        ivy {
            name = "OsmAndBinariesIvy"
            url = uri("https://builder.osmand.net")
            patternLayout {
                artifact("ivy/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]")
            }
        }
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        id("de.undercouch.download") version "4.1.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        ivy {
            name = "OsmAndBinariesIvy"
            url = uri("https://builder.osmand.net")
            patternLayout {
                artifact("ivy/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]")
            }
        }
    }
}

rootProject.name = "Team-TX-Aragua-Unified"

extra["java_shared_conf"] = ""

include(":app")
project(":app").projectDir = file("Team-Nacional-TX-Aragua/app")

include(":mapa")
project(":mapa").projectDir = file("Team-Nacional-TX-Aragua/mapa")

// OsmAnd library modules
include(":OsmAnd-shared")
project(":OsmAnd-shared").projectDir = file("android/OsmAnd-shared")

include(":OsmAnd-java")
project(":OsmAnd-java").projectDir = file("android/OsmAnd-java")

include(":OsmAnd-api")
project(":OsmAnd-api").projectDir = file("android/OsmAnd-api")

include(":OsmAnd")
project(":OsmAnd").projectDir = file("android/OsmAnd")