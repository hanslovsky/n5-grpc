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

import com.google.gson.JsonElement
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.janelia.saalfeldlab.n5.grpc.N5GrpcReader
import org.janelia.saalfeldlab.n5.grpc.defaultGson
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

        Assertions.assertFalse(service.isTerminated)
        Assertions.assertFalse(channel.isTerminated)
        Assertions.assertFalse(service.isShutdown)
        Assertions.assertFalse(channel.isShutdown)
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
