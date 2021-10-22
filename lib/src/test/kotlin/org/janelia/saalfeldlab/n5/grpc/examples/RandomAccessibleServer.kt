package org.janelia.saalfeldlab.n5.grpc.examples

import com.google.protobuf.UnsafeByteOperations
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import net.imglib2.RandomAccessible
import net.imglib2.position.FunctionRandomAccessible
import net.imglib2.type.NativeType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.janelia.saalfeldlab.n5.*
import org.janelia.saalfeldlab.n5.grpc.N5GrpcReader
import org.janelia.saalfeldlab.n5.grpc.asMessage
import org.janelia.saalfeldlab.n5.grpc.defaultGson
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlin.math.min

class Element(vararg children: Pair<String, Element>) {

    var parent: Element? = null
        private set

    var dataset: Dataset<*>? = null

    private val children = mutableMapOf<String, Element>()

    val isDataset: Boolean get() = dataset != null

    init {
        children.forEach { (i, c) -> addChild(i, c) }
    }

    fun removeChild(identifier: String) = children.remove(identifier)?.let { it.parent = null }
    fun addChild(identifier: String, element: Element) {
        removeChild(identifier)
        children[identifier] = element.also { it.parent = this }
    }
    fun getChild(identifier: String): Element? = children[identifier]
    fun listChildren(): List<String> = children.keys.toList()

    class Dataset<T: NativeType<T>>(val ra: RandomAccessible<T>, val attributes: DatasetAttributes) {
        fun minFor(vararg gridPosition: Long) = LongArray(attributes.numDimensions) { gridPosition[it] * attributes.blockSize[it] }
        fun maxFor(vararg gridPosition: Long) = minFor(*gridPosition).let { m -> LongArray(m.size) { min(m[it] + attributes.blockSize[it], attributes.dimensions[it] - 1) } }
    }
}

fun Element.forPath(path: String, regex: Regex = "/".toRegex()) = forPath(path.split(regex))
fun Element.forPath(vararg pathElements: String) = forPath(pathElements.toList())
fun Element.forPath(pathElements: List<String>) = pathElements.fold(this as Element?) { c, s -> c?.getChild(s) }

fun Element.createAt(path: String, dataset: Element.Dataset<*>? = null, regex: Regex = "/".toRegex())= createAt(path.split(regex), dataset)
fun Element.createAt(vararg pathElements: String, dataset: Element.Dataset<*>? = null) = createAt(pathElements.toList(), dataset)
fun Element.createAt(pathElements: List<String>, dataset: Element.Dataset<*>? = null) = pathElements
    .fold(this) { c, s -> c.getChild(s) ?: Element().also { c.addChild(s, it) } }.also { it.dataset = dataset }

class N5CallbackWriter(private val callback: (String?, DataBlock<*>?) -> Unit) : N5Writer {
    override fun <T : Any?> getAttribute(pathName: String?, key: String?, clazz: Class<T>?): T = TODO("Not yet implemented")
    override fun <T : Any?> getAttribute(pathName: String?, key: String?, type: Type?): T = TODO("Not yet implemented")
    override fun getDatasetAttributes(pathName: String?): DatasetAttributes = TODO("Not yet implemented")
    override fun readBlock(pathName: String?, datasetAttributes: DatasetAttributes?, vararg gridPosition: Long): DataBlock<*> = TODO("Not yet implemented")
    override fun exists(pathName: String?): Boolean = TODO("Not yet implemented")
    override fun list
                (pathName: String?): Array<String> = TODO("Not yet implemented")
    override fun listAttributes(pathName: String?): MutableMap<String, Class<*>> = TODO("Not yet implemented")
    override fun setAttributes(pathName: String?, attributes: MutableMap<String, *>?) = Unit // TODO
    override fun createGroup(pathName: String?) = Unit // TODO
    override fun remove(pathName: String?): Boolean = TODO("Not yet implemented")
    override fun remove(): Boolean = TODO("Not yet implemented")
    override fun deleteBlock(pathName: String?, vararg gridPosition: Long): Boolean = TODO("Not yet implemented")

    override fun <T : Any?> writeBlock(pathName: String?, datasetAttributes: DatasetAttributes?, dataBlock: DataBlock<T>?) = callback(pathName, dataBlock)
}

class RandomAccessibleServer(serverBuilder: ServerBuilder<*>,
                             val port: Int,
                             vararg datasets: Pair<String, Element.Dataset<*>?>) {

    constructor(port: Int, vararg datasets: Pair<String, Element.Dataset<*>?>) : this(ServerBuilder.forPort(port), port, *datasets)

    private val server: Server
    private val root = Element().also { datasets.forEach { (p, d) -> it.createAt(p, d) } }

    init {
        val service = object : N5GRPCServiceGrpc.N5GRPCServiceImplBase() {
            override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {

                val path = request.path.pathName
                val group = root.forPath(path)
                val data = if (true == group?.isDataset) {
                    val dataset = group.dataset!!
                    val gridPosition = LongArray(request.gridPositionCount) { request.getGridPosition(it) }
                    val blocks = mutableListOf<DataBlock<*>?>()
                    val dummyWriter = N5CallbackWriter { _, block -> blocks += block }
                    val min = dataset.minFor(*gridPosition)
                    val max = dataset.maxFor(*gridPosition)
                    N5Utils.save(
                        Views.interval(dataset.ra, min, max),
                        dummyWriter,
                        path,
                        dataset.attributes.blockSize,
                        dataset.attributes.compression
                    )

                    val data = UnsafeByteArrayOutputStream().use {
                        DefaultBlockWriter.writeBlock(
                            it,
                            dataset.attributes,
                            blocks[0]
                        )
                        it.toByteArray()
                        UnsafeByteOperations.unsafeWrap(it.currentBufUnsafe, 0, it.currentCount)
                    }
                    data
                } else
                    null
                responseObserver.onNext(N5Grpc.Block.newBuilder().setData(data).build())
                responseObserver.onCompleted()
            }

            override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
                val jsonString = "{}"
                responseObserver.onNext(N5Grpc.JsonString.newBuilder().setJsonString(jsonString).build())
                responseObserver.onCompleted()
            }

            override fun exists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                val path = request?.pathName
                val exists = path != null && root.forPath(path) != null
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(exists).build())
                responseObserver.onCompleted()
            }

            override fun list(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.Paths>) {
                // TOOD: for now do not implement list in meaningful way
                val contents = listOf<String>()
                val contentsPaths = contents.map { N5Grpc.Path.newBuilder().setPathName(it).build() }
                responseObserver.onNext(N5Grpc.Paths.newBuilder().addAllPaths(contentsPaths).build())
                responseObserver.onCompleted()
            }

            override fun datasetExists(request: N5Grpc.Path?, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
                val path = request?.pathName
                val datasetExists = path != null && root.forPath(path)?.isDataset == true
                responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(datasetExists).build())
                responseObserver.onCompleted()
            }

            override fun getDatasetAttributes(
                request: N5Grpc.Path,
                responseObserver: StreamObserver<N5Grpc.DatasetAttributes>
            ) {
                responseObserver.onNext(root.forPath(request.pathName)?.dataset?.attributes?.asMessage())
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
                    this@RandomAccessibleServer.stop()
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

    private class UnsafeByteArrayOutputStream(size: Int = 32) : ByteArrayOutputStream(size) {
        val currentBufUnsafe get() = super.buf
        val currentCount get() = super.count
    }
}

fun main() {
    val server = RandomAccessibleServer(
        9090,
        "my/dataset" to Element.Dataset(
            FunctionRandomAccessible(1, { l, t -> t.setInteger(l.getLongPosition(0)) }) { UnsignedByteType() },
            DatasetAttributes(longArrayOf(23), intArrayOf(5), DataType.UINT8, Lz4Compression())),
        "my/group" to null
    )
    server.start()

    val reader = N5GrpcReader("localhost", 9090)
    println(reader.exists("my/no"))
    println(reader.exists("my/group"))
    println(reader.exists("my/dataset"))
    println(reader.datasetExists("my/group"))
    println(reader.datasetExists("my/dataset"))

    val attributes = reader.getDatasetAttributes("my/dataset")!!
    for (o in 0 until 5) {
        val data = reader.readBlock("my/dataset", attributes, o.toLong())
        println((data.data as ByteArray).joinToString(prefix = "[", postfix = "]"))
    }

//    val root = Element()
//    root.addChild("blub", Element("bleb" to Element("blub" to Element())))
//    println(root.forPath("blub")?.listChildren())
//    println(root.forPath("blub/bleb")?.listChildren())
//    println(root.forPath("blub/bleb/blub")?.listChildren())
//
//    root.createAt("oke", "oke", "oke")
//    println(root.forPath("oke", "oke")?.listChildren())
//    println(root.forPath("oke", "oke", "oke")?.listChildren())
}