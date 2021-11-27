plugins {
    kotlin("multiplatform")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}


val dataforgeVersion: String by rootProject.extra
val kmathVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api(project(":numass-data-model"))
            api("space.kscience:tables-kt:0.1.1-dev-2")
            api("space.kscience:kmath-complex:$kmathVersion")
            api("space.kscience:kmath-stat:$kmathVersion")
            api("space.kscience:kmath-histograms:$kmathVersion")
        }
    }
}


