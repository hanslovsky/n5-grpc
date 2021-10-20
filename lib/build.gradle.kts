// These import is necessary for the protoc task below
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.0"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    id("com.google.protobuf") version "0.8.17"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()

    // SciJava repo for N5
     add(maven("https://maven.scijava.org/content/groups/public"))
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // N5
    api("org.janelia.saalfeldlab:n5:2.5.1")

    // protobuf
    implementation("io.grpc:grpc-kotlin-stub:1.2.0")
    implementation("io.grpc:grpc-protobuf:1.41.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.19.0-rc-1")
    implementation("io.grpc:grpc-stub:1.41.0")

    // need netty server for tests/examples
    testImplementation("io.grpc:grpc-netty:1.41.0")

    // use n5-hdf5 for example
    testImplementation("org.janelia.saalfeldlab:n5-hdf5:1.4.1")

    // Use the Kotlin test library.
    // testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    // testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

}

// set up protobuf plugin
// TOOD how to fix issue "protoc: stdout: . stderr: protoc-gen-kotlin: program not found or is not executable
//                        --kotlin_out: protoc-gen-kotlin: Plugin failed with status code 1."
//      https://github.com/google/protobuf-gradle-plugin/issues/511
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.17.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.41.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.2.0:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            // TODO Do I need this?
//            it.builtins {
//                kotlin {}
//            }
        }
    }
}

// add proto source set
sourceSets {
    named("main") {
        java.srcDir("build/generated/source/proto/main/java")
        java.srcDir("build/generated/source/proto/main/grpc")
        java.srcDir("build/generated/source/proto/main/grpckt")
    }
}
