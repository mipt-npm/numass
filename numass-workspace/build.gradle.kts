plugins {
    kotlin("jvm")
    id("ru.mipt.npm.kscience")
}

kscience{
    application()
    publish()
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra("0.3.1-dev-5")
val kmathVersion: String by rootProject.extra("0.2.0-dev-6")

dependencies {
    implementation(project(":numass-data-proto"))
    implementation("hep.dataforge:dataforge-workspace:$dataforgeVersion")
    implementation("kscience.plotlykt:plotlykt-core:$plotlyVersion")
    implementation("kscience.kmath:kmath-histograms:$kmathVersion")
}