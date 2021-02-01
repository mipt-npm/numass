plugins {
    kotlin("multiplatform")
    id("ru.mipt.npm.kscience")
}

kscience {
    publish()
}

val dataforgeVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api("hep.dataforge:dataforge-context:$dataforgeVersion")
            api("hep.dataforge:dataforge-data:$dataforgeVersion")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
        }
    }
}


