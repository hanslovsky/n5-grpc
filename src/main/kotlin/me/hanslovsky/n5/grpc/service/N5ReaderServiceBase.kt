package me.hanslovsky.n5.grpc.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.protobuf.ByteString
import com.google.protobuf.NullValue
import io.grpc.stub.StreamObserver
import me.hanslovsky.n5.grpc.asDatasetAttributes
import me.hanslovsky.n5.grpc.asMessage
import me.hanslovsky.n5.grpc.asNullableMessage
import me.hanslovsky.n5.grpc.generated.N5GRPCServiceGrpc
import me.hanslovsky.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*
import java.io.ByteArrayOutputStream

abstract class N5ReaderServiceBase(val gson: Gson): N5GRPCServiceGrpc.N5GRPCServiceImplBase() {

    abstract fun readBlock(path: String, attributes: DatasetAttributes, vararg gridPosition: Long): DataBlock<*>?
    abstract fun getAttributes(path: String): Map<String, JsonElement>
    abstract fun getDatasetAttributes(path: String): DatasetAttributes?
    abstract fun exists(path: String): Boolean
    abstract fun datasetExists(path: String): Boolean
    abstract fun list(path: String): Array<String>

    override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.NullableBlock>) {
        val attributes = request.datasetAttributes.asDatasetAttributes(gson)
        val block = readBlock(
            request.path.pathName,
            attributes,
            *LongArray(request.gridPositionCount) { request.getGridPosition(it) }
        )
        responseObserver.onNext(block.asNullableMessage(attributes))
        responseObserver.onCompleted()
    }

    override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
        val attributes = getAttributes(request.pathName)
        val jsonString = attributes.toJsonString(gson)
        responseObserver.onNext(N5Grpc.JsonString.newBuilder().setJsonString(jsonString).build())
        responseObserver.onCompleted()
    }

    override fun getDatasetAttributes(
        request: N5Grpc.Path,
        responseObserver: StreamObserver<N5Grpc.NullableDatasetAttributes>
    ) {
        responseObserver.onNext(getDatasetAttributes(request.pathName).asNullableMessage(gson))
        responseObserver.onCompleted()
    }

    override fun exists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(exists(request.pathName)).build())
        responseObserver.onCompleted()
    }

    override fun list(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.Paths>) {
        val contents = list(request.pathName).map { N5Grpc.Path.newBuilder().setPathName(it).build() }
        responseObserver.onNext(N5Grpc.Paths.newBuilder().addAllPaths(contents).build())
        responseObserver.onCompleted()
    }

    override fun datasetExists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        responseObserver.onNext(N5Grpc.BooleanFlag.newBuilder().setFlag(datasetExists(request.pathName)).build())
        responseObserver.onCompleted()
    }

    override fun healthCheck(request: N5Grpc.HealthRequest?, responseObserver: StreamObserver<N5Grpc.HealthStatus>) {
        responseObserver.onNext(N5Grpc.HealthStatus.newBuilder().setStatus(N5Grpc.HealthStatus.Status.SERVING).build())
        responseObserver.onCompleted()
    }

}

private fun Map<String, *>.toJsonString(gson: Gson) = gson.toJson(this)