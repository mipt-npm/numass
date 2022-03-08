plugins {
    id("ru.mipt.npm.gradle.jvm")
    id("com.github.johnrengelman.shadow") version "7.1.1"
    `maven-publish`
}

kotlin {
    explicitApi = null
}

val dataforgeVersion: String by rootProject.extra
val plotlyVersion: String by rootProject.extra("0.5.0")
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
    implementation(platform("com.google.cloud:libraries-bom:23.0.0"))
    implementation("com.google.cloud:google-cloud-nio:0.123.10")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.3.0")
}

kscience{
    jupyterLibrary("ru.inr.mass.notebook.NumassJupyter")
}