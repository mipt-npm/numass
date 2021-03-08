plugins {
    kotlin("jvm")
    id("ru.mipt.npm.gradle.common")
    id("com.squareup.wire") version "3.5.0"
}

kscience{
    publish()
}

val dataforgeVersion: String by rootProject.extra

dependencies {
    api(project(":numass-data-model"))
    api("space.kscience:dataforge-io:$dataforgeVersion")
}

wire{
    kotlin{}
}
