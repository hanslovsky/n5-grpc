plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

dependencies {
    // N5
    implementation("org.janelia.saalfeldlab:n5")

    implementation(project(":n5-grpc"))

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