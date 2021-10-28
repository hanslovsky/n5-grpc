package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.grpc.StatusRuntimeException
import kotlin.reflect.full.isSubclassOf
import me.hanslovsky.n5.grpc.service.N5GrpcServer
import org.janelia.saalfeldlab.n5.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext


internal class N5GrpcReaderTest {

    private val reader = N5GrpcReader(host="localhost", port=9090)

    @ExtendWith(EnableGetDatasetAttributes::class)
    @Test
    fun getDatasetAttributes() {
        val attributes = reader.getDatasetAttributes(datasetPath) ?: error("Received null attributes.")
        Assertions.assertArrayEquals(dimensions, attributes.dimensions)
        Assertions.assertArrayEquals(blockSize, attributes.blockSize)
        Assertions.assertEquals(dataType, attributes.dataType)
        // Serialize because Lz4Compression uses default equals
        Assertions.assertEquals(
            reader.gson.toJsonTree(compression),
            reader.gson.toJsonTree(attributes.compression))
    }

    @ExtendWith(EnableGetDatasetAttributes::class)
    @Test
    fun getDatasetAttributesNotADataset() {
        Assertions.assertThrows(StatusRuntimeException::class.java) {
            reader.getDatasetAttributes(groupPath) ?: error("Received null attributes.")
        }
    }

    @ExtendWith(EnableReadBlock::class, EnableGetDatasetAttributes::class)
    @Test
    fun readBlock() {
        val readBlock = reader.readBlock(datasetPath, reader.getDatasetAttributes(datasetPath)!!, *blockPosition)
        Assertions.assertTrue(readBlock.data::class.isSubclassOf(block.data::class))
        Assertions.assertEquals(block.numElements, readBlock.numElements)
        Assertions.assertArrayEquals(block.size, readBlock.size)
        Assertions.assertArrayEquals(block.gridPosition, readBlock.gridPosition)
        Assertions.assertArrayEquals(block.data, readBlock.data as DoubleArray)
    }

    @ExtendWith(EnableExists::class)
    @Test
    fun exists() {
        Assertions.assertTrue(reader.datasetExists(groupPath))
        Assertions.assertFalse(reader.datasetExists(datasetPath))
    }

    @ExtendWith(EnableDatasetExists::class)
    @Test
    fun datasetExists() {
        Assertions.assertFalse(reader.datasetExists(groupPath))
        Assertions.assertTrue(reader.datasetExists(datasetPath))
    }

    @ExtendWith(EnableList::class)
    @Test
    fun list() {
        contents.forEach { k, v -> Assertions.assertArrayEquals(v, reader.list(k)) }
        Assertions.assertFalse("some/other/key" in contents)
        Assertions.assertArrayEquals(arrayOf(), reader.list("some/other/key"))
    }

    @ExtendWith(EnableGetAttributes::class)
    @Test
    fun getAttributes() {
        Assertions.assertEquals(fooBarAttributes, reader.getAttributes(groupPath))
        Assertions.assertEquals(emptyAttributes, reader.getAttributes(datasetPath))
    }

    class EnableGetDatasetAttributes : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) {
            val attributes = DatasetAttributes(dimensions, blockSize, dataType, compression)
            testService.getDatasetAttributes = { if (it == datasetPath)  attributes else error("Not a dataset") }
        }

        override fun afterTestExecution(context: ExtensionContext?) { testService.getDatasetAttributes = null }

    }

    class EnableReadBlock : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) {
            testService.readBlock = { path, attr, pos ->
                if (path == datasetPath && blockPosition.contentEquals(pos) && dataType == attr.dataType)
                    block
                else
                    null
            }
        }

        override fun afterTestExecution(context: ExtensionContext?) { testService.readBlock = null }

    }

    class EnableExists : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.datasetExists = { it == groupPath } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    class EnableDatasetExists : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.datasetExists = { it == datasetPath } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    class EnableList : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.list = { contents[it] ?: arrayOf() } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.list = null }
    }

    class EnableGetAttributes : AfterTestExecutionCallback, BeforeTestExecutionCallback {
        override fun beforeTestExecution(context: ExtensionContext?) { testService.getAttributes = { if (it == groupPath) fooBarAttributes else emptyAttributes } }
        override fun afterTestExecution(context: ExtensionContext?) { testService.datasetExists = null }
    }

    companion object {
        private val testService = TestService()
        private val server = N5GrpcServer(9090, testService)

        private val groupPath = "my/group"
        private val datasetPath = "my/dataset"

        private val contents = mapOf(
            "" to arrayOf("my"),
            "my" to arrayOf("group", "dataset"),
            "my/group" to arrayOf(""),
            "my/dataset" to arrayOf("")
        )

        private val fooBarAttributes = mapOf("foo" to JsonPrimitive("bar"))
        private val emptyAttributes = mapOf<String, JsonElement>()

        private val dimensions = longArrayOf(2, 3, 4)
        private val blockSize = intArrayOf(1, 2, 3)
        private val dataType = DataType.FLOAT64
        private val compression = Lz4Compression(1 shl 17)

        private val blockPosition = longArrayOf(0, 1, 0)
        private val blockDimensions = intArrayOf(1, 1, 3)
        private val blockContents = doubleArrayOf(1.0, 2.0, 3.0)
        private val block = DoubleArrayDataBlock(blockDimensions, blockPosition, blockContents)

        @JvmStatic
        @BeforeAll
        fun startServer() = server.start()

        @JvmStatic
        @AfterAll
        fun stopServer() = server.stop()
    }
}