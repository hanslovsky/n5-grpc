package org.janelia.saalfeldlab.n5.grpc.examples.server

import com.google.gson.Gson
import org.janelia.saalfeldlab.n5.GsonAttributesParser
import org.janelia.saalfeldlab.n5.grpc.N5GrpcReader
import org.janelia.saalfeldlab.n5.grpc.service.N5ReaderService
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader
import picocli.CommandLine
import java.util.concurrent.Callable


class ServerBackedByN5: Callable<Int> {

    @CommandLine.Option(names = ["--hdf5-file", "-f"], required = true)
    private lateinit var hdf5Path: String

    @CommandLine.Option(names =["--default-block-size", "-b"], required=false, defaultValue = "16", split = ",")
    private lateinit var defaultBlockSize: IntArray

    @CommandLine.Option(names = ["--port", "-p"], required=false, defaultValue = defaultPort)
    private var port: Int = defaultPort.toInt()

    val defaultBlockSize3D get() = defaultBlockSize
        .also { require(it.size in setOf(1, 3)) {"Default block size can only be specified as 1D or 3D but got ${it.joinToString()}"} }
        .takeIf { it.size ==3 } ?: IntArray(3) { defaultBlockSize[0] }

    override fun call(): Int {
        val h5Reader = N5HDF5Reader(
            hdf5Path,
            true,
            *defaultBlockSize3D)
        val server = N5ReaderService.serve(h5Reader as GsonAttributesParser, 9090)
        server.start()
        val reader = N5GrpcReader("localhost", 9090)
        println("Contents of hdf file $hdf5Path (datasets highlighted by *)")
        val stack = mutableListOf("/")
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val isDataset = reader.datasetExists(current)
            println("$current${if (isDataset) "*" else ""}")
            if (!isDataset)
                stack.addAll(reader.list(current).map { "${if (current == "/") "" else current}/$it" })
        }
        SigintLog.waitBlocking()
        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine(ServerBackedByN5()).execute(*args)
        }

        private fun Map<String, *>.toJsonString(gson: Gson) = gson.toJson(this)

        private const val defaultPort = "9090"
    }
}