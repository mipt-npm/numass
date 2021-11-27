plugins {
    kotlin("jvm")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}

kotlin {
    explicitApi = null
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra
val kmathVersion: String by rootProject.extra

dependencies {
    implementation(project(":numass-data-proto"))
    implementation(project(":numass-model"))
    implementation(project(":numass-analysis"))
    implementation("space.kscience:dataforge-workspace:$dataforgeVersion")
    implementation("space.kscience:plotlykt-core:$plotlyVersion")
    implementation("space.kscience:kmath-histograms:$kmathVersion")
    implementation("space.kscience:kmath-for-real:$kmathVersion")
}