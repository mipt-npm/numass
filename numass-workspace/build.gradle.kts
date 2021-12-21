plugins {
    id("ru.mipt.npm.gradle.jvm")
    id("com.github.johnrengelman.shadow") version "7.1.1"
    `maven-publish`
}

kotlin {
    explicitApi = null
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra
val kmathVersion: String by rootProject.extra
val tablesVersion: String by rootProject.extra

dependencies {
    implementation(projects.numassDataProto)
    implementation(projects.numassModel)
    implementation(projects.numassAnalysis)
    implementation("space.kscience:dataforge-workspace:$dataforgeVersion")
    implementation("space.kscience:plotlykt-jupyter:$plotlyVersion")
    implementation("space.kscience:kmath-jupyter:$kmathVersion")
    implementation("space.kscience:tables-kt:$tablesVersion")
}

kscience{
    jupyterLibrary("ru.inr.mass.notebook.NumassJupyter")
}