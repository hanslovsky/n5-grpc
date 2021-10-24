package me.hanslovsky.n5.grpc

import me.hanslovsky.n5.grpc.service.N5GrpcServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class N5GrpcReaderTest {

    @Test
    fun getDatasetAttributes() {
        TODO("Do this")
    }

    @Test
    fun readBlock() {
        TODO("Do this")
    }

    @Test
    fun exists() {
        val server = N5GrpcServer(9090, TestService(datasetExists = { N5Grpc.BooleanFlag.newBuilder().setFlag((it.pathName == "my/group")).build() }))
        val reader = N5GrpcReader(host="localhost", port=9090)
        server.start()
        Assertions.assertTrue(reader.datasetExists("my/group"))
        Assertions.assertFalse(reader.datasetExists("my/dataset"))
        server.stop()
    }

    @Test
    fun datasetExists() {
        val server = N5GrpcServer(9090, TestService(datasetExists = { N5Grpc.BooleanFlag.newBuilder().setFlag((it.pathName == "my/dataset")).build() }))
        val reader = N5GrpcReader(host="localhost", port=9090)
        server.start()
        Assertions.assertFalse(reader.datasetExists("my/group"))
        Assertions.assertTrue(reader.datasetExists("my/dataset"))
        server.stop()
    }

    @Test
    fun list() {
        TODO("Do this")
    }

    @Test
    fun getAttributes() {
        TODO("Do this")
    }
}