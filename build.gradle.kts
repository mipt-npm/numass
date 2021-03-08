plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    repositories{
        mavenLocal()
        maven("https://repo.kotlin.link")
    }

    group = "ru.inr.mass"
    version = "0.1.0-SNAPSHOT"
}

val dataforgeVersion by extra("0.4.0-dev-2")
val kmathVersion by extra("0.2.1")

apiValidation{
    validationDisabled = true
}

ksciencePublish{
    configurePublications("https://mipt-npm.jetbrains.space/p/numass/code/numass/")
    space("https://maven.pkg.jetbrains.space/mipt-npm/p/numass/maven")
}