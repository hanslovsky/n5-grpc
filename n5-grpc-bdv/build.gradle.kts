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
    implementation("org.janelia.saalfeldlab:n5")

    implementation(project(":lib"))

    // need netty server for tests/examples
    implementation("io.grpc:grpc-netty:1.41.0")

    // use n5-hdf5 for example
    implementation("org.janelia.saalfeldlab:n5-hdf5")

    // imglib2 for function server
    implementation("net.imglib2:imglib2:")
    implementation("org.janelia.saalfeldlab:n5-imglib2")

    // CLI parser
    implementation("info.picocli:picocli")

    // bdv
    implementation("sc.fiji:bigdataviewer-vistools")
}

application {
    mainClass.set(project.properties["mainClass"]?.toString() ?: "me.hanslovsky.n5.grpc.bdv.VisualizeWithBdv")
}