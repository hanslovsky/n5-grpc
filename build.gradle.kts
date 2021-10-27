plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.0" apply false

    // use this to include pom-scijava bom
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply true
}


allprojects {

    System.setProperty("kotlin.version", "1.5.0")

    repositories {
        // Use Maven Central for resolving dependencies.
        mavenCentral()

        // SciJava repo for N5
        add(maven("https://maven.scijava.org/content/groups/public"))
    }

    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom("org.scijava:pom-scijava:31.1.0")
        }
    }
}
