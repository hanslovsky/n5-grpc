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
package org.janelia.saalfeldlab.n5.grpc

import com.google.gson.JsonElement
import com.google.protobuf.NullValue
import org.janelia.saalfeldlab.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class N5GrpcUtilTest {

    @Test
    fun `test DatasetAttributes asMessage`() {
        // Given
        val attributes = DatasetAttributes(dimensions, blockSize, dataType, compression)

        // When
        val actual = attributes.asMessage()

        // Then
        actual.assertExpected()
    }

    @Test
    fun `test null DatasetAttributes asNullableMessage`() {
        // Given
        val attributes: DatasetAttributes? = null

        // When
        val message = attributes.asNullableMessage()

        // Then
        Assertions.assertTrue(message.hasEmpty())
        Assertions.assertEquals(NullValue.NULL_VALUE, message.empty)
    }

    @Test
    fun `test null Message asDatasetAttributes`() {
        // Given
        val message = N5Grpc.NullableDatasetAttributes.newBuilder().setEmpty(NullValue.NULL_VALUE).build()

        // When
        val attributes = message.asDatasetAttributesOrNull()

        // Then
        Assertions.assertNull(attributes)
    }

    @Test
    fun `test nullable Message asDatasetAttributes`() {
        // Given
        val message = DatasetAttributes(dimensions, blockSize, dataType, compression).asNullableMessage()

        // When
        val attributes = message.asDatasetAttributesOrNull()

        // Then
        Assertions.assertNotNull(attributes)
    }

    @Test
    fun `test Message asDatasetAttributes`() {
        // Given
        val message = DatasetAttributes(dimensions, blockSize, dataType, compression).asMessage()

        // When
        val attributes = message.asDatasetAttributes()

        // Then
        attributes.assertExpected()
    }

    @Test
    fun `test asPath`() {
        // Given
        val path = "path"

        // When
        val message = path.asPath()

        // Then
        Assertions.assertEquals(path, message.pathName)
    }

    @Test
    fun `test null asPath`() {
        // Given
        val path: String? = null

        // When
        val message = path.asPath()

        // Then
        Assertions.assertEquals("/", message.pathName)
    }

    @Test
    fun `test null block as message`() {
        // Given
        val block: DataBlock<*>? = null

        // When
        val message = block.asNullableMessage(null)

        // Then
        Assertions.assertTrue(message.hasEmpty())
    }

    @Test
    fun `test null message as block`() {
        // Given
        val message = N5Grpc.NullableBlock.newBuilder().setEmpty(NullValue.NULL_VALUE).build()

        // When
        val block = message.asDataBlockOrNull(null)

        // Then
        Assertions.assertNull(block)
    }

    companion object {
        private val dimensions = longArrayOf(2, 3)
        private val blockSize = intArrayOf(2, 2)
        private val dataType = DataType.UINT16
        private val compression = Lz4Compression(1 shl 17)

       private fun N5Grpc.DatasetAttributes.assertExpected() {
            Assertions.assertArrayEquals(Companion.dimensions, dimensionsList.toLongArray())
            Assertions.assertArrayEquals(Companion.blockSize, blockSizeList.toIntArray())
            Assertions.assertEquals(Companion.dataType, DataType.fromString(dataType))
            Assertions.assertEquals(
                    defaultGson.toJsonTree(Companion.compression),
                    defaultGson.fromJson(compressionJsonString, JsonElement::class.java))
        }

        private fun DatasetAttributes.assertExpected() {
            Assertions.assertArrayEquals(Companion.dimensions, dimensions)
            Assertions.assertArrayEquals(Companion.blockSize, blockSize)
            Assertions.assertEquals(Companion.dataType, dataType)
            Assertions.assertEquals(defaultGson.toJsonTree(Companion.compression), defaultGson.toJsonTree(compression))
        }
    }
}
