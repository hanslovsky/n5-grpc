package me.hanslovsky.n5.grpc

import N5GRPCServiceGrpc
import N5Grpc
import io.grpc.stub.StreamObserver

class TestService(
    private val readBlock: ((N5Grpc.BlockMeta) -> N5Grpc.Block)? = null,
    private val getAttributes: ((N5Grpc.Path) -> N5Grpc.JsonString)? = null,
    private val getDatasetAttributes: ((N5Grpc.Path) -> N5Grpc.DatasetAttributes)? = null,
    private val exists: ((N5Grpc.Path) -> N5Grpc.BooleanFlag)? = null,
    private val list: ((N5Grpc.Path) -> N5Grpc.Paths)? = null,
    private val datasetExists: ((N5Grpc.Path) ->N5Grpc.BooleanFlag)? = null
) : N5GRPCServiceGrpc.N5GRPCServiceImplBase() {
    override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {
        if (readBlock == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(readBlock.invoke(request))
    }

    override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
        if (getAttributes == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(getAttributes.invoke(request))
    }

    override fun getDatasetAttributes(
        request: N5Grpc.Path,
        responseObserver: StreamObserver<N5Grpc.DatasetAttributes>
    ) {
        if (getDatasetAttributes == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(getDatasetAttributes.invoke(request))
    }

    override fun exists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        if (exists == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(exists.invoke(request))
    }

    override fun list(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.Paths>) {
        if (list == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(list.invoke(request))
    }

    override fun datasetExists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        if (datasetExists == null) TODO("Implementation not provided")
        responseObserver.onNextAndOnCompleted(datasetExists.invoke(request))
    }
}

fun <T> StreamObserver<T>.onNextAndOnCompleted(data: T) {
    onNext(data)
    onCompleted()
}