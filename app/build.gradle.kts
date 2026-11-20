@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.util.Properties

val aboutLibsVersion = "13.1.0" // keep in sync with plugin version
val kotlinVersion = "2.3.0"

plugins {
    id("com.android.application")
    id("com.android.built-in-kotlin")
    id("androidx.baselineprofile")
    kotlin("plugin.parcelize")
    kotlin("plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    id("pt.jcosta.resourceplaceholders")
}

android {
    val packageProperties = readProperties(file("../package.properties"))
    fun resolveProperties(property: String): String? {
        return if (project.hasProperty(property)) project.properties[property].toString()
        else packageProperties.getOrDefault(property, null) as String?
    }

    val releaseType = resolveProperties("releaseType")!!
    val appIdOverride = resolveProperties("appIdOverride")
    val vnos = resolveProperties("version" + "NameSuffixOverride")

    val myVersionName = "." + (vnos ?: "git rev-parse --short=7 HEAD".runCommand(workingDir = rootDir))
    if (releaseType.contains("\"")) {
        throw IllegalArgumentException("releaseType must not contain \"")
    }

    namespace = "org.akanework.gramophone"
    compileSdk = 37

    signingConfigs {
        create("release") {
            if (resolveProperties("AKANE_RELEASE_KEY_ALIAS") != null) {
                storeFile = file(resolveProperties("AKANE_RELEASE_STORE_FILE")!!)
                storePassword = resolveProperties("AKANE_RELEASE_STORE_PASSWORD")
                keyAlias = resolveProperties("AKANE_RELEASE_KEY_ALIAS")
                keyPassword = resolveProperties("AKANE_RELEASE_KEY_PASSWORD")
            }
        }
        create("release2") {
            if (resolveProperties("AKANE2_RELEASE_KEY_ALIAS")!= null) {
                storeFile = file(resolveProperties("AKANE2_RELEASE_STORE_FILE")!!)
                storePassword = resolveProperties("AKANE2_RELEASE_STORE_PASSWORD")
                keyAlias = resolveProperties("AKANE2_RELEASE_KEY_ALIAS")
                keyPassword = resolveProperties("AKANE2_RELEASE_KEY_PASSWORD")
            }
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
        prefab = true
        compose = true
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
        jniLibs {
            useLegacyPackaging = false
            // https://issuetracker.google.com/issues/168777344#comment11
            pickFirsts += "lib/arm64-v8a/libdlfunc.so"
            pickFirsts += "lib/armeabi-v7a/libdlfunc.so"
            pickFirsts += "lib/x86/libdlfunc.so"
            pickFirsts += "lib/x86_64/libdlfunc.so"
        }
        resources {
            // https://youtrack.jetbrains.com/issue/KT-48019/Bundle-Kotlin-Tooling-Metadata-into-apk-artifacts
            excludes += "kotlin-tooling-metadata.json"
            // https://issuetracker.google.com/issues/152898926#comment7
            excludes += "META-INF/*.version"
            // https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
            excludes += "DebugProbesKt.bin"
            // covered by AboutLicenses instead
            excludes += "META-INF/androidx/*/*/LICENSE.txt"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
