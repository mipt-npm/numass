plugins {
    id("ru.mipt.npm.gradle.jvm")
}

val dataforgeVersion: String by rootProject.extra

dependencies {
//    implementation("io.ktor:ktor-client-cio:$ktorVersion")
//    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation(npmlibs.ktor.client.cio)
    api(projects.numassDataProto)
}