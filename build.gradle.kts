plugins {
    id("ru.mipt.npm.project")
}

allprojects {
    group = "ru.inr.mass"
    version = "0.1.0"
}

val dataforgeVersion by extra("0.3.0-dev-1")

apiValidation{
    validationDisabled = true
}

val vcs by project.extra("https://mipt-npm.jetbrains.space/p/numass/code/numass/")

ksciencePublish{
    spaceRepo = "https://maven.pkg.jetbrains.space/mipt-npm/p/numass/maven"
}