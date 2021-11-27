plugins {
    kotlin("multiplatform")
    id("ru.mipt.npm.gradle.common")
    `maven-publish`
}

val visionForgeVersion = "0.2.0-dev-24"

kotlin {
    js{
        browser {
            webpackTask {
                this.outputFileName = "js/numass-web.js"
            }
        }
        binaries.executable()
    }

    afterEvaluate {
        val jsBrowserDistribution by tasks.getting

        tasks.getByName<ProcessResources>("jvmProcessResources") {
            dependsOn(jsBrowserDistribution)
            afterEvaluate {
                from(jsBrowserDistribution)
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":numass-data-model"))
                implementation("space.kscience:visionforge-plotly:$visionForgeVersion")
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":numass-data-proto"))
                implementation("space.kscience:visionforge-server:$visionForgeVersion")
            }
        }

    }
}

kscience{
    useSerialization {
        json()
    }
}
