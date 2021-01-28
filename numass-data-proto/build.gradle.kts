plugins {
    kotlin("jvm")
    id("ru.mipt.npm.kscience")
    id("com.squareup.wire") version "3.5.0"
}

kscience{
    publish()
}

val dataforgeVersion: String by rootProject.extra

dependencies {
    api(project(":numass-data-model"))
    api("hep.dataforge:dataforge-workspace:$dataforgeVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.1")
}

wire{
    kotlin{}
}
