plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven("https://repo.kotlin.link")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("com.squareup.wire:wire-gradle-plugin:3.7.1")
    api("ru.mipt.npm:gradle-tools:0.10.7")
}
