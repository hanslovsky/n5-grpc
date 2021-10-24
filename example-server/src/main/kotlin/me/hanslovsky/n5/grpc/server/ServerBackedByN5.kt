package me.hanslovsky.n5.grpc.server

import N5GRPCServiceGrpc
import N5Grpc
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.janelia.saalfeldlab.n5.*
import me.hanslovsky.n5.grpc.N5GrpcReader
import me.hanslovsky.n5.grpc.asDatasetAttributes
import me.hanslovsky.n5.grpc.asMessage
import me.hanslovsky.n5.grpc.defaultGson
import me.hanslovsky.n5.grpc.service.N5ReaderService
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


class ServerBackedByN5(serverBuilder: ServerBuilder<*>, val port: Int, val reader: GsonAttributesParser) {



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val h5Reader = N5HDF5Reader("/home/zottel/Downloads/sample_A_20160501.hdf", true, 16, 16, 16)
            val server = N5ReaderService.serve(h5Reader, 9090)
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
            val data = reader.readBlock("/volumes/raw", dsAttributes, 0, 0, 0)
            println((data.data as ByteArray).joinToString(prefix = "[", postfix = "]"))
            println((data.data as ByteArray).size)
        }

        private fun Map<String, *>.toJsonString(gson: Gson) = gson.toJson(this)
    }
}