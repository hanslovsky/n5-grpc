package me.hansllovsky.n5.grpc

import N5GRPCServiceGrpc
import N5Grpc
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.janelia.saalfeldlab.n5.*
import java.io.ByteArrayInputStream
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
        stub.getDatasetAttributes(pathName.asPath())?.asDatasetAttributes(gson)

    override fun readBlock(
        pathName: String?,
        datasetAttributes: DatasetAttributes,
        vararg gridPosition: Long
    ): DataBlock<*> {
        val builder = N5Grpc.BlockMeta.newBuilder().also { builder ->
            builder.path = pathName.asPath()
            gridPosition.forEach { builder.addGridPosition(it) }
            datasetAttributes.let { builder.datasetAttributes = it.asMessage(gson) }
        }
        val meta = builder.build()
        val response = stub.readBlock(meta)
        val block = DefaultBlockReader.readBlock(
            // TODO can we do this without array() call?
            ByteArrayInputStream(response.data.asByteArray()),
            datasetAttributes,
            gridPosition
        )
        // TODO do we need to do verifications on block?
        return block
    }

    override fun exists(pathName: String?) =  stub.exists(pathName.asPath()).flag

    override fun datasetExists(pathName: String?) = stub.datasetExists(pathName.asPath()).flag

    override fun list(pathName: String?) = stub.list(pathName.asPath()).pathsList.map { it.pathName }.toTypedArray()

    override fun getAttributes(pathName: String?) =
        GsonAttributesParser.readAttributes(stub.getAttributes(pathName.asPath()).reader(), gson)

    companion object {

    }
}