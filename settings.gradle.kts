pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }

    val toolsVersion = "0.9.2"
    val kotlinVersion = "1.4.31"

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
        id("ru.mipt.npm.gradle.publish") version toolsVersion
        kotlin("jvm") version kotlinVersion
        kotlin("js") version kotlinVersion
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.squareup.wire") {
                // For some reason, Gradle does a lookup on the wrong coordinates:
                // 'com.squareup.wire:com.squareup.wire.gradle.plugin' instead of the one below.
                useModule("com.squareup.wire:wire-gradle-plugin:${requested.version}")
            }
        }
    }
}

include("numass-data-model")
include("numass-data-proto")
include("numass-workspace")
