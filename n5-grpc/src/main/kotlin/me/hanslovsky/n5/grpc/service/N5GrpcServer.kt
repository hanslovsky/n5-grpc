package me.hanslovsky.n5.grpc.service

import io.grpc.ServerBuilder
import java.util.concurrent.TimeUnit

class N5GrpcServer(serverBuilder: ServerBuilder<*>, service: N5GRPCServiceGrpc.N5GRPCServiceImplBase) {

    constructor(port: Int, service: N5GRPCServiceGrpc.N5GRPCServiceImplBase) : this(ServerBuilder.forPort(port), service)

    private val server = serverBuilder.addService(service).build()

    fun start() {
        server.start()
        println("Server started, listening on ${server.port}")
        Runtime.getRuntime().addShutdownHook(Thread {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down")
            try {
                this@N5GrpcServer.stop()
            } catch (e: InterruptedException) {
                e.printStackTrace(System.err)
            }
            System.err.println("*** server shut down")
        })
    }

    @Throws(InterruptedException::class)
    fun stop() {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS)
    }
}