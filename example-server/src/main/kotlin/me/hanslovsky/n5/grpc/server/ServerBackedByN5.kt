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
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


class ServerBackedByN5(serverBuilder: ServerBuilder<*>, val port: Int, val reader: GsonAttributesParser) {

    constructor(port: Int, reader: GsonAttributesParser) : this(ServerBuilder.forPort(port), port, reader)

    private val server: Server
    private val gson get() = if (reader is AbstractGsonReader) reader.gson else defaultGson

    init {
        val service = object : N5GRPCServiceGrpc.N5GRPCServiceImplBase() {
            override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {
                val attributes =
                    if (request.hasDatasetAttributes()) request.datasetAttributes.asDatasetAttributes(gson)
                    else reader.getDatasetAttributes(request.path.pathName)
                val block = reader.readBlock(request.path.pathName, attributes, *LongArray(request.gridPositionCount) { request.getGridPosition(it) })
                val data = ByteArrayOutputStream().use {
                    DefaultBlockWriter.writeBlock(it, attributes, block)
                    it.toByteArray()
                }
                responseObserver.onNext(N5Grpc.Block.newBuilder().setData(ByteString.copyFrom(data)).build())
                responseObserver.onCompleted()
            }

            override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
                val attributes = reader.getAttributes(request.pathName)
                val jsonString = attributes.toJsonString(gson)
                responseObserver.onNext(N5Grpc.JsonString.newBuilder().setJsonString(jsonString).build())
                responseObserver.onCompleted()
            }

            override fun getDatasetAttributes(
                request: N5Grpc.Path?,
                responseObserver: StreamObserver<N5Grpc.DatasetAttributes>
            ) {
                responseObserver.onNext(reader.getDatasetAttributes(request?.pathName).asMessage(gson))
                responseObserver.onCompleted()
            }

            override fun exists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(reader.exists(request?.pathName)).build())
                responseObserver.onCompleted()
            }

            override fun list(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.Paths>) {
                val contents = reader.list(request?.pathName)
                val contentsPaths = contents.map { N5Grpc.Path.newBuilder().setPathName(it).build() }
                responseObserver.onNext(N5Grpc.Paths.newBuilder().addAllPaths(contentsPaths).build())
                responseObserver.onCompleted()
            }

            override fun datasetExists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(reader.datasetExists(request?.pathName)).build())
                responseObserver.onCompleted()
            }
        }
        this.server = serverBuilder.addService(service).build()
    }

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down")
                try {
                    this@ServerBackedByN5.stop()
                } catch (e: InterruptedException) {
                    e.printStackTrace(System.err)
                }
                System.err.println("*** server shut down")
            }
        })
    }

    @Throws(InterruptedException::class)
    fun stop() {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val h5Reader = N5HDF5Reader("/home/zottel/Downloads/sample_A_20160501.hdf", true, 16, 16, 16)
            val server = ServerBackedByN5(9090, h5Reader)
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