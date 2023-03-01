/*-
 * #%L
 * N5 gRPC
 * %%
 * Copyright (C) 2021 - 2023 Stephan Saalfeld
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.janelia.saalfeldlab.n5.grpc.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.grpc.stub.StreamObserver
import org.janelia.saalfeldlab.n5.grpc.asDatasetAttributes
import org.janelia.saalfeldlab.n5.grpc.asNullableMessage
import org.janelia.saalfeldlab.n5.grpc.generated.N5GRPCServiceGrpc
import org.janelia.saalfeldlab.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*

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
