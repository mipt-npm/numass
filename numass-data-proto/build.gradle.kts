import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    java
    kotlin("jvm")
    id("ru.mipt.npm.kscience")
    id("com.google.protobuf") version "0.8.14"
}

kscience{
    publish()
}

val dataforgeVersion: String by rootProject.extra

dependencies {
    api(project(":numass-data-model"))
    api("hep.dataforge:dataforge-workspace:$dataforgeVersion")
    implementation("com.google.protobuf:protobuf-java:3.14.0")
    implementation("javax.annotation:javax.annotation-api:1.3.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(":numass-data-proto:generateProto")
}

sourceSets {
    create("proto") {
        proto {
            srcDir("src/main/proto")
        }
    }
    create("gen"){
        java{
            srcDir("gen/main/java")
        }
    }
}

//kotlin{
//    sourceSets{
//        main{
//            de
//        }
//    }
//}


protobuf {
    // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.14.0"
    }

    generatedFilesBaseDir = "$projectDir/gen"
}
