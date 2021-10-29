// These imports are necessary for the protoc task below
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("org.jetbrains.kotlin.jvm")

    id("com.google.protobuf") version "0.8.17"

    jacoco
    `java-library`
    `maven-publish`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // N5
    api("org.janelia.saalfeldlab:n5")

    // protobuf
    implementation("io.grpc:grpc-kotlin-stub:1.2.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.19.0-rc-1")
    api("io.grpc:grpc-protobuf:1.41.0")
    api("io.grpc:grpc-stub:1.41.0")

    testImplementation(kotlin("test"))
    testImplementation("io.grpc:grpc-core:1.41.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
}

tasks.test {
    useJUnitPlatform()
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.hanslovsky"
            artifactId = "n5-grpc"
            version = "0.1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}

// jacoco configuration
tasks.test {
    configure<JacocoTaskExtension> {
        // TODO For some reason, exclude does not work.
        //      Leave it in here for reference
        exclude("**/generated/**/*class")
        exclude("me.hanslovsky.n5.grpc.generated.*")
    }
}

tasks.jacocoTestReport {

    dependsOn(tasks.test)
    sourceSets(sourceSets.main.get())
    // exclude does not work. Exclude class files explicitly
    classDirectories.setFrom(
            listOf("java", "kotlin")
                    .map { fileTree("build/classes/$it/main") }
                    .map { it.filter { f -> !f.path.contains("me/hanslovsky/n5/grpc/generated") }}
    )

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
    }
}
