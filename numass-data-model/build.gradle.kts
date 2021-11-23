plugins {
    id("ru.mipt.npm.gradle.mpp")
    `maven-publish`
}


val dataforgeVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api("space.kscience:dataforge-context:$dataforgeVersion")
            api("space.kscience:dataforge-data:$dataforgeVersion")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
        }
    }
    jvmMain{
        dependencies{
            api("ch.qos.logback:logback-classic:1.2.3")
        }
    }
}


