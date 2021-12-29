package me.hanslovsky.n5.grpc.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import me.hanslovsky.n5.grpc.N5GrpcReader
import me.hanslovsky.n5.grpc.defaultGson
import org.janelia.saalfeldlab.n5.DataBlock
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.GsonAttributesParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.util.UUID

internal class N5ReaderServiceTest {
    @Test
    fun testService() {
        val reader = N5GrpcReader(channel)

        Assertions.assertFalse(reader.exists(""))
        Assertions.assertFalse(reader.datasetExists(""))
        Assertions.assertArrayEquals(arrayOf(), reader.list(""))
        Assertions.assertEquals(mapOf<String, JsonElement>(), reader.getAttributes(""))
        // TODO Assertions.assertNull(reader.getDatasetAttributes(""))
    }

    private class DummyReader : GsonAttributesParser {
        override fun <T : Any?> getAttribute(pathName: String?, key: String?, clazz: Class<T>?): T? = null
        override fun <T : Any?> getAttribute(pathName: String?, key: String?, type: Type?): T? = null
        override fun getDatasetAttributes(pathName: String?): DatasetAttributes? = null
        override fun readBlock(pathName: String?, datasetAttributes: DatasetAttributes?, vararg gridPosition: Long): DataBlock<*>? = null
        override fun exists(pathName: String?) = false
        override fun datasetExists(pathName: String?) = false
        override fun list(pathName: String?) = arrayOf<String>()
        override fun getGson() = defaultGson
        override fun getAttributes(pathName: String?) = hashMapOf<String, JsonElement>()

    }

    companion object {
        private val name = "n5-reader-service-${UUID.randomUUID()}"
        private val service = N5ReaderService.serve(DummyReader(), InProcessServerBuilder.forName(name))
        private val channel = InProcessChannelBuilder.forName(name).directExecutor().build()

        @JvmStatic
        @BeforeAll
        fun startServer() = service.start()

        @JvmStatic
        @AfterAll
        fun stopServer() {
            channel.shutdown()
            service.stop()
        }
    }
}