package org.janelia.saalfeldlab.n5.grpc

import com.google.gson.GsonBuilder
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.janelia.saalfeldlab.n5.grpc.generated.N5GRPCServiceGrpc
import org.janelia.saalfeldlab.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*
import java.lang.reflect.Type

class N5GrpcReader private constructor(
        private val stub: N5GRPCServiceGrpc.N5GRPCServiceBlockingStub,
        gsonBuilder: GsonBuilder = defaultGsonBuilder
) : GsonAttributesParser, N5Reader {

    // We cannot override getGson using a field named gson, unfortunately.
    private val _gson = gsonBuilder.create()
    override fun getGson() = _gson

    @JvmOverloads constructor(
        host: String,
        port: Int,
        gsonBuilder: GsonBuilder = defaultGsonBuilder
    ) : this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(), gsonBuilder)

    @JvmOverloads constructor(
        target: String,
        gsonBuilder: GsonBuilder = defaultGsonBuilder
    ) : this(ManagedChannelBuilder.forTarget(target).usePlaintext(), gsonBuilder)

    @JvmOverloads constructor(
        channelBuilder: ManagedChannelBuilder<*>,
        gsonBuilder: GsonBuilder = defaultGsonBuilder
    ) : this(channelBuilder.build(), gsonBuilder)

    @JvmOverloads constructor(
        channel: ManagedChannel,
        gsonBuilder: GsonBuilder = defaultGsonBuilder
    ) : this(N5GRPCServiceGrpc.newBlockingStub(channel), gsonBuilder)

    override fun <T : Any?> getAttribute(pathName: String?, key: String?, clazz: Class<T>?): T =
        GsonAttributesParser.parseAttribute(getAttributes(pathName), key, clazz, gson)

    override fun <T : Any?> getAttribute(pathName: String?, key: String?, type: Type?): T =
        GsonAttributesParser.parseAttribute(getAttributes(pathName), key, type, gson)

    override fun getDatasetAttributes(pathName: String?): DatasetAttributes? =
        stub.getDatasetAttributes(pathName.asPath())?.asDatasetAttributesOrNull(gson)

    override fun readBlock(
        pathName: String?,
        datasetAttributes: DatasetAttributes,
        vararg gridPosition: Long
    ): DataBlock<*>? {
        val builder = N5Grpc.BlockMeta.newBuilder().also { builder ->
            builder.path = pathName.asPath()
            gridPosition.forEach { builder.addGridPosition(it) }
            datasetAttributes.let { builder.datasetAttributes = it.asMessage(gson) }
        }
        val meta = builder.build()
        val response = stub.readBlock(meta)
        return response.asDataBlockOrNull(datasetAttributes, *gridPosition)
    }

    override fun exists(pathName: String?) =  stub.exists(pathName.asPath()).flag

    override fun datasetExists(pathName: String?) = stub.datasetExists(pathName.asPath()).flag

    override fun list(pathName: String?) = stub.list(pathName.asPath()).pathsList.map { it.pathName }.toTypedArray()

    override fun getAttributes(pathName: String?) =
        GsonAttributesParser.readAttributes(stub.getAttributes(pathName.asPath()).reader(), gson)

    fun healthCheck(): N5Grpc.HealthStatus.Status {
        return stub.healthCheck(N5Grpc.HealthRequest.newBuilder().build()).status
    }

    class AutoCloseableReader private constructor(
            private val channel: ManagedChannel,
            gsonBuilder: GsonBuilder = defaultGsonBuilder,
            // delegate needs to be defined in constructor for delegation
            private val delegate: N5GrpcReader = N5GrpcReader(N5GRPCServiceGrpc.newBlockingStub(channel), gsonBuilder)
    ) : GsonAttributesParser by delegate, N5Reader {

        @JvmOverloads constructor(
                host: String,
                port: Int,
                gsonBuilder: GsonBuilder = defaultGsonBuilder
        ) : this(ManagedChannelBuilder.forAddress(host, port), gsonBuilder)

        @JvmOverloads constructor(
                target: String,
                gsonBuilder: GsonBuilder = defaultGsonBuilder
        ) : this(ManagedChannelBuilder.forTarget(target), gsonBuilder)

        @JvmOverloads constructor(
                channelBuilder: ManagedChannelBuilder<*>,
                gsonBuilder: GsonBuilder = defaultGsonBuilder
        ) : this(channelBuilder.usePlaintext().build(), gsonBuilder)

        val isClosed: Boolean get() = channel.isShutdown

        override fun close() {
            channel.shutdown()
        }

        fun healthCheck() = delegate.healthCheck()

    }
}