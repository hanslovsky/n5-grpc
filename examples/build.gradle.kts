plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()

    // SciJava repo for N5
    add(maven("https://maven.scijava.org/content/groups/public"))
}

dependencies {


    // N5
    implementation("org.janelia.saalfeldlab:n5:2.5.1")

    implementation(project(":lib"))

    // need netty server for tests/examples
    implementation("io.grpc:grpc-netty:1.41.0")

    // use n5-hdf5 for example
    implementation("org.janelia.saalfeldlab:n5-hdf5:1.4.1")

    // imglib2 for function server
    implementation("net.imglib2:imglib2:5.12.0")
    implementation("org.janelia.saalfeldlab:n5-imglib2:4.1.1")

}

application {
    mainClass.set(project.properties["mainClass"]?.toString() ?: "me.hanslovsky.n5.grpc.examples.RandomAccessibleServerKt")
}