plugins {
    kotlin("jvm")
    id("ru.mipt.npm.gradle.common")
    id("com.squareup.wire") version "4.2.0"
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra

dependencies {
    api(project(":numass-data-model"))
    api("space.kscience:dataforge-io:$dataforgeVersion")
}

wire{
    kotlin{}
}
