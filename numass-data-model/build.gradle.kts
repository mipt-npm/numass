plugins {
    kotlin("multiplatform")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}


val dataforgeVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api("space.kscience:dataforge-context:$dataforgeVersion")
            api("space.kscience:dataforge-data:$dataforgeVersion")
            api("org.jetbrains.kotlinx:kotlinx-datetime:${ru.mipt.npm.gradle.KScienceVersions.dateTimeVersion}")
        }
    }
    jvmMain{
        dependencies{
            api("ch.qos.logback:logback-classic:1.2.3")
        }
    }
}

kscience{
    useSerialization()
}


