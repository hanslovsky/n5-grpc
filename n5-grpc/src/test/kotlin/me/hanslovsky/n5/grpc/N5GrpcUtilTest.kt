package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import me.hanslovsky.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.Compression
import org.janelia.saalfeldlab.n5.DataType
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.Lz4Compression
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class N5GrpcUtilTest {

    @Test
    fun `test DatasetAttributes asMessage`() {
        // Given
        val dimensions = longArrayOf(2, 3)
        val blockSize = intArrayOf(2, 2)
        val dataType = DataType.UINT16
        val compression = Lz4Compression(1 shl 17)
        val attributes = DatasetAttributes(dimensions, blockSize, dataType, compression)

        // When
        val actual = attributes.asMessage()

        // Then
        Assertions.assertArrayEquals(dimensions, actual.dimensionsList.toLongArray())
        Assertions.assertArrayEquals(blockSize, actual.blockSizeList.toIntArray())
        Assertions.assertEquals(dataType, DataType.fromString(actual.dataType))
        Assertions.assertEquals(
                defaultGson.toJsonTree(compression),
                defaultGson.fromJson(actual.compressionJsonString, JsonElement::class.java))
    }
}