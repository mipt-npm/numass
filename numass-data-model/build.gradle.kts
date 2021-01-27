plugins {
    kotlin("jvm")
    id("ru.mipt.npm.kscience")
}

kscience{
    publish()
}

val dataforgeVersion: String by rootProject.extra

dependencies {
    api("hep.dataforge:dataforge-context:$dataforgeVersion")
}
