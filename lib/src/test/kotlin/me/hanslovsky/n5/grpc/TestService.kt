package me.hanslovsky.n5.grpc

import N5GRPCServiceGrpc
import N5Grpc
import io.grpc.stub.StreamObserver

class TestService(
    var readBlock: ((N5Grpc.BlockMeta) -> N5Grpc.Block)? = null,
    var getAttributes: ((N5Grpc.Path) -> N5Grpc.JsonString)? = null,
    var getDatasetAttributes: ((N5Grpc.Path) -> N5Grpc.DatasetAttributes)? = null,
    var exists: ((N5Grpc.Path) -> N5Grpc.BooleanFlag)? = null,
    var list: ((N5Grpc.Path) -> N5Grpc.Paths)? = null,
    var datasetExists: ((N5Grpc.Path) ->N5Grpc.BooleanFlag)? = null
) : N5GRPCServiceGrpc.N5GRPCServiceImplBase() {
    override fun readBlock(request: N5Grpc.BlockMeta, responseObserver: StreamObserver<N5Grpc.Block>) {
        readBlock.let { readBlock ->
            if (readBlock == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(readBlock.invoke(request))
        }
    }

    override fun getAttributes(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.JsonString>) {
        getAttributes.let { getAttributes ->
            if (getAttributes == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(getAttributes.invoke(request))
        }
    }

    override fun getDatasetAttributes(
        request: N5Grpc.Path,
        responseObserver: StreamObserver<N5Grpc.DatasetAttributes>
    ) {
        getDatasetAttributes.let { getDatasetAttributes ->
            if (getDatasetAttributes == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(getDatasetAttributes.invoke(request))
        }
    }

    override fun exists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        exists.let { exists ->
            if (exists == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(exists.invoke(request))
        }
    }

    override fun list(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.Paths>) {
        list.let { list ->
            if (list == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(list.invoke(request))
        }
    }

    override fun datasetExists(request: N5Grpc.Path, responseObserver: StreamObserver<N5Grpc.BooleanFlag>) {
        datasetExists.let { datasetExists ->
            if (datasetExists == null) TODO("Implementation not provided")
            responseObserver.onNextAndOnCompleted(datasetExists.invoke(request))
        }
    }
}

fun <T> StreamObserver<T>.onNextAndOnCompleted(data: T) {
    onNext(data)
    onCompleted()
}