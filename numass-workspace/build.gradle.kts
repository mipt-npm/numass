plugins {
    kotlin("jvm")
    id("ru.mipt.npm.gradle.common")
    id("com.github.johnrengelman.shadow") version "6.1.0"
    `maven-publish`
}

kotlin {
    explicitApi = null
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra("0.4.0-dev-1")
val kmathVersion: String by rootProject.extra

dependencies {
    implementation(project(":numass-data-proto"))
    implementation("space.kscience:dataforge-workspace:$dataforgeVersion")
    implementation("space.kscience:plotlykt-core:$plotlyVersion")
    implementation("space.kscience:kmath-histograms:$kmathVersion")
    implementation("space.kscience:kmath-for-real:$kmathVersion")
}