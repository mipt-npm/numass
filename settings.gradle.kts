pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://dl.bintray.com/mipt-npm/dataforge")
        maven("https://dl.bintray.com/mipt-npm/kscience")
        maven("https://dl.bintray.com/mipt-npm/dev")
    }

    val toolsVersion = "0.7.3-1.4.30-RC"
    val kotlinVersion = "1.4.30-RC"

    plugins {
        id("ru.mipt.npm.project") version toolsVersion
        id("ru.mipt.npm.mpp") version toolsVersion
        id("ru.mipt.npm.jvm") version toolsVersion
        id("ru.mipt.npm.js") version toolsVersion
        id("ru.mipt.npm.publish") version toolsVersion
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
