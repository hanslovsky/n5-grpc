package me.hanslovsky.n5.grpc.examples

import N5GRPCServiceGrpc
import N5Grpc
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.janelia.saalfeldlab.n5.DataType
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.RawCompression
import me.hanslovsky.n5.grpc.N5GrpcReader
import me.hanslovsky.n5.grpc.asMessage
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit


class DummyServer(
    serverBuilder: ServerBuilder<*>,
    val port: Int,
    val datasetPath: String = "abc",
    val dimensions: LongArray = longArrayOf(1, 1, 3),
    val blockSize: IntArray = dimensions.map { it.toInt() }.toIntArray(),
    val dataType: DataType = DataType.UINT8
) {

    constructor(
        port: Int,
        datasetPath: String = "abc",
        dimensions: LongArray = longArrayOf(1, 1, 3),
        blockSize: IntArray = dimensions.map { it.toInt() }.toIntArray(),
        dataType: DataType = DataType.UINT8) :
            this(ServerBuilder.forPort(port), port, datasetPath, dimensions, blockSize, dataType)

    private val server: Server

    init {
        val service = object : N5GRPCServiceGrpc.N5GRPCServiceImplBase() {
            override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {
                val dataBuffer = ByteBuffer.allocate(
                    0
                            + 2 // mode
                            + 2 // number of dimensions of block
                            + 4 * blockSize.size // dimensions of block
                            + dataType.sizeInBytes * blockSize.reduce { p, d -> p*d } // data
                )
                dataBuffer.putShort(0) // mode
                dataBuffer.putShort(blockSize.size.toShort()) // number of dimensions of block
                blockSize.forEach { dataBuffer.putInt(it) } // dimensions of block
                val putValue: (Int) -> Unit = when(dataType) {
                    DataType.INT8, DataType.UINT8 -> { v: Int -> dataBuffer.put(v.toByte()) }
                    DataType.INT16, DataType.UINT16 -> { v: Int -> dataBuffer.putShort(v.toShort()) }
                    DataType.INT32, DataType.UINT32 -> { v: Int -> dataBuffer.putInt(v) }
                    DataType.INT64, DataType.UINT64 -> { v: Int -> dataBuffer.putLong(v.toLong()) }
                    DataType.FLOAT32 -> { v: Int -> dataBuffer.putFloat(v.toFloat()) }
                    DataType.FLOAT64 -> { v: Int -> dataBuffer.putDouble(v.toDouble()) }
                    DataType.OBJECT -> error("Object datatype not supported")
                }

                for (v in 0 until blockSize.reduce { p, d -> p*d })
                    putValue(v)

                val data = dataBuffer.array()
                responseObserver.onNext(N5Grpc.Block.newBuilder().setData(ByteString.copyFrom(data)).build())
                responseObserver.onCompleted()
            }

            override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
                val jsonString = if (request.pathName == datasetPath)
                    mapOf(
                        "dimensions" to dimensions,
                        "blockSize" to blockSize,
                        "dataType" to dataType.toString(),
                        "compressionType" to "raw"
                    ).toJsonString()
                else
                    "{}"
                responseObserver.onNext(N5Grpc.JsonString.newBuilder().setJsonString(jsonString).build())
                responseObserver.onCompleted()
            }

            override fun exists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(true).build())
                responseObserver.onCompleted()
            }

            override fun list(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.Paths>) {
                val contents = if (datasetPath.startsWith(request?.pathName ?: ""))
                    listOf<String>(datasetPath)
                else
                    listOf<String>()
                val contentsPaths = contents.map { N5Grpc.Path.newBuilder().setPathName(it).build() }
                responseObserver.onNext(N5Grpc.Paths.newBuilder().addAllPaths(contentsPaths).build())
                responseObserver.onCompleted()
            }

            override fun datasetExists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(datasetPath == request?.pathName).build())
                responseObserver.onCompleted()
            }

            override fun getDatasetAttributes(
                request: N5Grpc.Path?,
                responseObserver: StreamObserver<N5Grpc.DatasetAttributes>
            ) {
                responseObserver.onNext(DatasetAttributes(dimensions, blockSize, dataType, RawCompression()).asMessage())
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
                    this@DummyServer.stop()
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
            val server = DummyServer(9090, dimensions = longArrayOf(5, 6), dataType = DataType.FLOAT64)
            server.start()
            val reader = N5GrpcReader("localhost", 9090)
            println(reader.exists("def"))
            println(reader.datasetExists("def"))
            println(reader.list("def").toList())
            println(reader.exists("abc"))
            println(reader.datasetExists("abc"))
            println(reader.list("a").toList())
            println(reader.list("ab").toList())
            println(reader.list("abc").toList())
            println(reader.list("abcd").toList())
            val attributes = reader.getDatasetAttributes("abc")!!
            val data = reader.readBlock("abc", attributes, 0, 0, 0)
            println((data.data as DoubleArray).joinToString(prefix = "[", postfix = "]"))
        }

        private fun Map<String, *>.toJsonString(gson: Gson = GsonBuilder().create()) = gson.toJson(this)
        private val DataType.sizeInBytes get() = when(this) {
            DataType.INT8, DataType.UINT8 -> 8
            DataType.INT16, DataType.UINT16 -> 16
            DataType.INT32, DataType.UINT32, DataType.FLOAT32 -> 32
            DataType.INT64, DataType.UINT64, DataType.FLOAT64 -> 64
            DataType.OBJECT -> error("Object datatype not supported")
        }
    }
}