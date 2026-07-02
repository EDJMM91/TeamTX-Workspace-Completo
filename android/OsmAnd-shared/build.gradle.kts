import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("org.jetbrains.kotlin.multiplatform")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("com.android.library")
	id("maven-publish")
	id("ivy-publish")
}

group = "net.osmand.shared"

kotlin {
	jvm {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
			freeCompilerArgs.add("-Xjvm-default=all")
		}
	}

	androidTarget {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
			freeCompilerArgs.add("-Xjvm-default=all")
		}
		publishLibraryVariants("release", "debug")
	}

	listOf(
		iosX64(),
		iosArm64(),
		iosSimulatorArm64()
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "OsmAndShared"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(libs.kotlin.stdlib)
			implementation(libs.kotlinx.serialization.json)
			implementation(libs.kotlinx.coroutines.core)
			implementation(libs.kotlinx.datetime)
			implementation(libs.okio)
			implementation(libs.stately.concurrent.collections)
			implementation(libs.ktor.client.core)
			implementation(libs.ktor.client.encoding)
			implementation(libs.ktor.network)
		}
		jvmMain.dependencies {
			//implementation(kotlin("stdlib-jdk8"))
			implementation(libs.kxml2)
			implementation(libs.sqlite.jdbc)
			implementation(libs.commons.logging)
            implementation(libs.ktor.client.okhttp)
		}
		androidMain.dependencies {
			implementation(libs.androidx.sqlite)
			implementation(libs.androidx.sqlite.framework)
			implementation(libs.kxml2)
			implementation(libs.coil.core)
			implementation(libs.coil.network.okhttp)
            implementation(libs.ktor.client.okhttp)
		}
		iosMain.dependencies {
			implementation(libs.sqliter.driver)
            implementation(libs.ktor.client.darwin)
		}

		commonTest.dependencies {
			implementation(kotlin("test", version = "2.0.0"))
            implementation(libs.ktor.client.mock)
		}
	}
}

android {
	namespace = "net.osmand.shared"
	compileSdk = 35
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	defaultConfig {
		minSdk = 24
	}
}

version = System.getenv("OSMAND_SHARED_ANDROID_BINARIES_IVY_REVISION") ?: "master-snapshot"
publishing {
	repositories {
		ivy {
			url = uri(System.getenv("OSMAND_BINARIES_IVY_ROOT") ?: "./")
		}
	}
	publications {
		create<IvyPublication>("ivyOsmAndSharedAndroid") {
			organisation = "net.osmand.shared"
			module = "OsmAnd-shared-android"
			revision = "$version"
			artifact(file("build/outputs/aar/OsmAnd-shared-debug.aar")) {
				type = "aar"
				classifier = "debug"
			}
			artifact(file("build/outputs/aar/OsmAnd-shared-release.aar")) {
				type = "aar"
				classifier = "release"
			}
		}
	}
}

tasks.named("publishIvyOsmAndSharedAndroidPublicationToIvyRepository") {
	dependsOn("bundleDebugAar", "bundleReleaseAar")
}
