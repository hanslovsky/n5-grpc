package me.hanslovsky.n5.grpc.service

import N5GRPCServiceGrpc
import N5Grpc
import com.google.gson.Gson
import com.google.protobuf.ByteString
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import me.hanslovsky.n5.grpc.asDatasetAttributes
import me.hanslovsky.n5.grpc.asMessage
import me.hanslovsky.n5.grpc.defaultGson
import org.janelia.saalfeldlab.n5.AbstractGsonReader
import org.janelia.saalfeldlab.n5.DefaultBlockWriter
import org.janelia.saalfeldlab.n5.GsonAttributesParser
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class N5ReaderService(private val reader: GsonAttributesParser): N5GRPCServiceGrpc.N5GRPCServiceImplBase() {

    val gson: Gson get() = if (reader is AbstractGsonReader) reader.gson else defaultGson

    override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {
        val attributes =
            if (request.hasDatasetAttributes()) request.datasetAttributes.asDatasetAttributes(gson)
            else reader.getDatasetAttributes(request.path.pathName)
        val block = reader.readBlock(
            request.path.pathName,
            attributes,
            *LongArray(request.gridPositionCount) { request.getGridPosition(it) })
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
        responseObserver.onNext(
            N5Grpc.BooleanFlag.newBuilder().setFlag(reader.datasetExists(request?.pathName)).build()
        )
        responseObserver.onCompleted()
    }

    companion object {
        fun serve(reader: GsonAttributesParser, serverBuilder: ServerBuilder<*>) = N5GrpcServer(serverBuilder, N5ReaderService(reader))
        fun serve(reader: GsonAttributesParser, port: Int) = N5GrpcServer(port, N5ReaderService(reader))
    }

}

private fun Map<String, *>.toJsonString(gson: Gson) = gson.toJson(this)