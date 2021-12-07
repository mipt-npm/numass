enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
        mavenCentral()
    }

    versionCatalogs {
        create("npm") {
            from("ru.mipt.npm:version-catalog:0.10.7")
        }
    }
}

include(
    ":numass-data-model",
    ":numass-analysis",
    ":numass-data-proto",
    //":numass-data-server",
    ":numass-workspace",
    ":numass-model"
)
