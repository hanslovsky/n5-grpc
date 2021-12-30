package me.hanslovsky.n5.grpc.service

import io.grpc.ServerBuilder
import me.hanslovsky.n5.grpc.generated.N5GRPCServiceGrpc
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
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
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