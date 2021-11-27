plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
    }

    group = "ru.inr.mass"
    version = "0.1.0-dev-1"
}

val dataforgeVersion by extra("0.5.2-dev-4")
val kmathVersion by extra("0.3.0-dev-17")
val plotlyVersion: String by extra("0.5.0")

ksciencePublish{
    git("https://mipt-npm.jetbrains.space/p/numass/code/numass/")
    space("https://maven.pkg.jetbrains.space/mipt-npm/p/numass/maven")
}