pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    val toolsVersion = "0.10.7"
    val kotlinVersion = "1.6.0"

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
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

include(
    ":numass-data-model",
    ":numass-analysis",
    ":numass-data-proto",
    ":numass-workspace",
    ":numass-model"
)
