package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import me.hanslovsky.n5.grpc.service.N5GrpcServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class N5GrpcReaderTest {



    @Test
    fun getDatasetAttributes() {
//        TODO("Do this")
    }

    @Test
    fun readBlock() {
//        TODO("Do this")
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
//        TODO("Do this")
    }

    @Test
    fun getAttributes() {
        val server = N5GrpcServer(9090, TestService(getAttributes = { N5Grpc.JsonString.newBuilder().setJsonString(if (it.pathName == "my/group") "{\"foo\":\"bar\"}" else "{}").build() }))
        val reader = N5GrpcReader(host="localhost", port=9090)
        server.start()
        println(defaultGson.toJson(mapOf("foo" to "bar")))
        println("{\"foo\":\"bar\"}")
        println(defaultGson.toJson(mapOf("foo" to "bar")) == "{\"foo\":\"bar\"}")
        Assertions.assertEquals(mapOf("foo" to JsonPrimitive("bar")), reader.getAttributes("my/group"))
        Assertions.assertEquals(mapOf<String, JsonElement>(), reader.getAttributes("my/dataset"))
        server.stop()
    }
}