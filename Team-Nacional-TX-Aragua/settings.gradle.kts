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
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://builder.osmand.net/maven2")
    }
    // Repo Ivy del motor OsmAnd (artefactos como net.osmand:antpluginlib)
    ivy {
      url = uri("https://builder.osmand.net")
      patternLayout {
        artifact("ivy/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]")
      }
      metadataSources { artifact() }
    }
    // Requerido por dependencias del motor OsmAnd (com.github.*)
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "Team TX Venezuela"

// Variable compartida usada por el motor OsmAnd (equivalente de android/settings.gradle)
gradle.extra["java_shared_conf"] = ""

include(":app")

// Motor de mapas OsmAnd — restaurado según última compilación exitosa (fuentes en ../android)
include(":OsmAnd")
project(":OsmAnd").projectDir = file("../android/OsmAnd")
include(":OsmAnd-api")
project(":OsmAnd-api").projectDir = file("../android/OsmAnd-api")
include(":OsmAnd-java")
project(":OsmAnd-java").projectDir = file("../android/OsmAnd-java")
include(":OsmAnd-shared")
project(":OsmAnd-shared").projectDir = file("../android/OsmAnd-shared")


