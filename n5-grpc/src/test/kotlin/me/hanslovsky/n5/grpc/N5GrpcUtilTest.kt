package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import com.google.protobuf.NullValue
import me.hanslovsky.n5.grpc.generated.N5Grpc
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