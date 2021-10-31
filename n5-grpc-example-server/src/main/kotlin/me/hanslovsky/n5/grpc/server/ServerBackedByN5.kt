package me.hanslovsky.n5.grpc.server

import com.google.gson.Gson
import io.grpc.ServerBuilder
import me.hanslovsky.n5.grpc.N5GrpcReader
import me.hanslovsky.n5.grpc.service.N5ReaderService
import org.janelia.saalfeldlab.n5.GsonAttributesParser
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader


class ServerBackedByN5(serverBuilder: ServerBuilder<*>, val port: Int, val reader: GsonAttributesParser) {



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val h5Reader = N5HDF5Reader("/home/zottel/Downloads/sample_A_20160501.hdf", true, 16, 16, 16)
            val server = N5ReaderService.serve(h5Reader as GsonAttributesParser, 9090)
            server.start()
            val reader = N5GrpcReader("localhost", 9090)
            val stack = mutableListOf("/")
            while (stack.isNotEmpty()) {
                val current = stack.removeLast()
                val isDataset = reader.datasetExists(current)
                println("$current${if (isDataset) "*" else ""}")
                if (!isDataset)
                    stack.addAll(reader.list(current).map { "${if (current == "/") "" else current}/$it" })
            }

            val attributes = reader.getAttributes("/volumes/raw")
            val dsAttributes = reader.getDatasetAttributes("/volumes/raw")!!
            println("attributes=$attributes")
            println("dsAttributes=$dsAttributes")
            val data = reader.readBlock("/volumes/raw", dsAttributes, 0, 0, 0)!!
            println((data.data as ByteArray).joinToString(prefix = "[", postfix = "]"))
            println((data.data as ByteArray).size)
        }

        private fun Map<String, *>.toJsonString(gson: Gson) = gson.toJson(this)
    }
}