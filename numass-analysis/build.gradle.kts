plugins {
    id("ru.mipt.npm.gradle.mpp")
    `maven-publish`
}


val dataforgeVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api(project(":numass-data-model"))
            api("space.kscience:tables-kt:0.1.1-dev-2")
            api("space.kscience:kmath-complex:0.3.0-dev-17")
            api("space.kscience:kmath-stat:0.3.0-dev-17")
            api("space.kscience:kmath-histograms:0.3.0-dev-17")
        }
    }
}


