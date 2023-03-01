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

import io.grpc.ServerBuilder
import org.janelia.saalfeldlab.n5.grpc.generated.N5GRPCServiceGrpc
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.TimeUnit

class N5GrpcServer(serverBuilder: ServerBuilder<*>, service: N5GRPCServiceGrpc.N5GRPCServiceImplBase) {

    constructor(port: Int, service: N5GRPCServiceGrpc.N5GRPCServiceImplBase) : this(ServerBuilder.forPort(port), service)

    private val server = serverBuilder.addService(service).build()

    val isShutdown get() = server.isShutdown
    val isTerminated get() = server.isTerminated

    fun start() {
        server.start()
        logger.info("Server started, listening on ${server.port}")
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("*** shutting down gRPC server since JVM is shutting down")
            try {
                this@N5GrpcServer.stop()
            } catch (e: InterruptedException) {
                e.printStackTrace(System.err)
            }
            logger.info("*** server shut down")
        })
    }

    @Throws(InterruptedException::class)
    fun stop() {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
