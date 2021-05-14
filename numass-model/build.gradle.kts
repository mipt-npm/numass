plugins {
    kotlin("multiplatform")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}


val dataforgeVersion: String by rootProject.extra
val kmathVersion: String by rootProject.extra

kotlin.sourceSets {
    commonMain {
        dependencies {
            api("space.kscience:dataforge-context:$dataforgeVersion")
            api("space.kscience:kmath-for-real:$kmathVersion")
            api("space.kscience:kmath-functions:$kmathVersion")
        }
    }
    jvmMain {
        dependencies {
            api("space.kscience:kmath-commons:$kmathVersion")
            api("ch.qos.logback:logback-classic:1.2.3")
        }
    }
}
