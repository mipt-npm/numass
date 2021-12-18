plugins {
    kotlin("jvm")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}

kotlin {
    explicitApi = null
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra
val kmathVersion: String by rootProject.extra

dependencies {
    implementation(projects.numassDataProto)
    implementation(projects.numassModel)
    implementation(projects.numassAnalysis)
    implementation("space.kscience:dataforge-workspace:$dataforgeVersion")
    implementation("space.kscience:plotlykt-jupyter:$plotlyVersion")
    implementation("space.kscience:kmath-jupyter:$kmathVersion")
}

kscience{
    jupyterLibrary("ru.inr.mass.notebook.NumassJupyter")
}