plugins {
    id("ru.mipt.npm.project")
}

group = "ru.inr.mass"
version = "0.1.0-SHAPSHOT"

val dataforgeVersion by extra("0.3.0-dev")

val spaceRepo by extra("https://maven.pkg.jetbrains.space/mipt-npm/p/numass/maven")

apiValidation{
    validationDisabled = true
}