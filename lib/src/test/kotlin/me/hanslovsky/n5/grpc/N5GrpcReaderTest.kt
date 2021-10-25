package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.hanslovsky.n5.grpc.service.N5GrpcServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext


internal class N5GrpcReaderTest {

    @Test
    fun getDatasetAttributes() {
//        TODO("Do this")
    }

    @Test
    fun readBlock() {
//        TODO("Do this")
    }

    @ExtendWith(EnableExists::class)
    @Test
    fun exists() {
        val reader = N5GrpcReader(host="localhost", port=9090)
        Assertions.assertTrue(reader.datasetExists("my/group"))
        Assertions.assertFalse(reader.datasetExists("my/dataset"))
    }

    @ExtendWith(EnableDatasetExists::class)
    @Test
    fun datasetExists() {
        val reader = N5GrpcReader(host="localhost", port=9090)
        Assertions.assertFalse(reader.datasetExists("my/group"))
        Assertions.assertTrue(reader.datasetExists("my/dataset"))
    }

    @Test
    fun list() {
//        TODO("Do this")
    }

    @ExtendWith(EnableGetAttributes::class)
    @Test
    fun getAttributes() {
        val reader = N5GrpcReader(host="localhost", port=9090)
        Assertions.assertEquals(mapOf("foo" to JsonPrimitive("bar")), reader.getAttributes("my/group"))
        Assertions.assertEquals(mapOf<String, JsonElement>(), reader.getAttributes("my/dataset"))
    }

    class EnableGetDatasetAttributes

    class EnableReadBlock

    class EnableExists : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.datasetExists = { N5Grpc.BooleanFlag.newBuilder().setFlag((it.pathName == "my/group")).build() } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    class EnableDatasetExists : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.datasetExists = { N5Grpc.BooleanFlag.newBuilder().setFlag((it.pathName == "my/dataset")).build() } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    class EnableList

    class EnableGetAttributes : AfterTestExecutionCallback, BeforeTestExecutionCallback{
        override fun beforeTestExecution(context: ExtensionContext?) { testService.getAttributes = { N5Grpc.JsonString.newBuilder().setJsonString(if (it.pathName == "my/group") "{\"foo\":\"bar\"}" else "{}").build() } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    companion object {
        private val testService = TestService()
        private val server = N5GrpcServer(9090, testService)

        @JvmStatic
        @BeforeAll
        fun startServer() = server.start()

        @JvmStatic
        @AfterAll
        fun stopServer() = server.stop()
    }
}