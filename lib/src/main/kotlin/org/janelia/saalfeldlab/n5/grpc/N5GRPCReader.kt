package org.janelia.saalfeldlab.n5.grpc

import N5GRPCServiceGrpc
import N5Grpc
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.AbstractBlockingStub
import io.grpc.stub.StreamObserver
import org.janelia.saalfeldlab.n5.*
import java.io.ByteArrayInputStream
import java.lang.reflect.Type
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.HashMap

class N5GRPCReader private constructor(
    private val stub: N5GRPCServiceGrpc.N5GRPCServiceBlockingStub,
    gsonBuilder: GsonBuilder) : AbstractGsonReader(gsonBuilder), N5Reader {

    @JvmOverloads constructor(
        host: String,
        port: Int,
        gsonBuilder: GsonBuilder = GsonBuilder()) : this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(), gsonBuilder)

    @JvmOverloads constructor(
        target: String,
        gsonBuilder: GsonBuilder = GsonBuilder()) : this(ManagedChannelBuilder.forTarget(target).usePlaintext(), gsonBuilder)

    @JvmOverloads constructor(
        channelBuilder: ManagedChannelBuilder<*>,
        gsonBuilder: GsonBuilder = GsonBuilder()) : this(channelBuilder.build(), gsonBuilder)

    @JvmOverloads constructor(
        channel: ManagedChannel,
        gsonBuilder: GsonBuilder = GsonBuilder()) : this(N5GRPCServiceGrpc.newBlockingStub(channel), gsonBuilder)

    override fun readBlock(
        pathName: String?,
        datasetAttributes: DatasetAttributes?,
        vararg gridPosition: Long
    ): DataBlock<*> {
        val builder = N5Grpc.BlockMeta.newBuilder().also { builder ->
            builder.path = pathName.asPath()
            gridPosition.forEach { builder.addGridPosition(it) }
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

    override fun list(pathName: String?) = stub.list(pathName.asPath()).pathsList.map { it.pathName }.toTypedArray()

    override fun getAttributes(pathName: String?) = GsonAttributesParser.readAttributes(stub.listAttributes(pathName.asPath()).reader(), gson)

    companion object {
        // TODO should we fail on null or default to "/"?
        private fun String?.asPath() = N5Grpc.Path.newBuilder().setPathName(this ?: "/").build()
        private fun N5Grpc.JsonString.reader() = jsonString.reader()
        private fun ByteBuffer.asByteArray() = ByteArray(capacity()) { this[it] }
        private fun ByteString.asByteArray() = asReadOnlyByteBuffer().asByteArray()
    }
}