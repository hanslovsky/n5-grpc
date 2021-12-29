package me.hanslovsky.n5.grpc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import com.google.protobuf.NullValue
import com.google.protobuf.UnsafeByteOperations
import me.hanslovsky.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

// TODO should we fail on null or default to "/"?
fun String?.asPath() = N5Grpc.Path.newBuilder().setPathName(this ?: "/").build()
fun N5Grpc.JsonString.reader() = jsonString.reader()
fun ByteBuffer.asByteArray() = ByteArray(limit()).also { this[it] }
fun ByteString.asByteArray() = asReadOnlyByteBuffer().asByteArray()

fun DatasetAttributes?.asNullableMessage(gson: Gson = defaultGson): N5Grpc.NullableDatasetAttributes {
    val builder = N5Grpc.NullableDatasetAttributes.newBuilder()
    if (this == null)
        builder.empty = NullValue.NULL_VALUE
    else
        builder.datasetAttributes = asMessage(gson)
    return builder.build()
}

fun DatasetAttributes.asMessage(gson: Gson = defaultGson): N5Grpc.DatasetAttributes {
    return N5Grpc.DatasetAttributes.newBuilder().also { b ->
        dimensions.forEach { b.addDimensions(it) }
        blockSize.forEach { b.addBlockSize(it) }
        b.dataType = dataType.toString()
        b.compressionJsonString = compression.toJson(gson)
    }.build()
}

fun N5Grpc.NullableDatasetAttributes.asDatasetAttributesOrNull(gson: Gson = defaultGson) = when {
    this.hasEmpty() -> null
    else -> datasetAttributes.asDatasetAttributes(gson)
}

fun N5Grpc.DatasetAttributes.asDatasetAttributes(gson: Gson = defaultGson) = DatasetAttributes(
    LongArray(dimensionsCount) { getDimensions(it) },
    IntArray(blockSizeCount) { getBlockSize(it) },
    DataType.fromString(dataType),
    compressionJsonString.toCompression(gson)
)

private fun Compression.toJson(gson: Gson) = gson.toJson(this)
private fun String.toCompression(gson: Gson) = gson.fromJson(this, Compression::class.java)

val defaultGsonBuilder: GsonBuilder get() = GsonBuilder()
    .registerTypeAdapter(DataType::class.java, DataType.JsonAdapter())
    .registerTypeHierarchyAdapter(Compression::class.java, CompressionAdapter.getJsonAdapter())
    .disableHtmlEscaping()

val defaultGson = defaultGsonBuilder.create()

fun DataBlock<*>?.asNullableMessage(attributes: DatasetAttributes?): N5Grpc.NullableBlock {
    val builder = N5Grpc.NullableBlock.newBuilder()
    if (this == null)
        builder.setEmpty(NullValue.NULL_VALUE)
    else
        builder.setBlock(asMessage(attributes!!))
    return builder.build()
}

fun DataBlock<*>.asMessage(attributes: DatasetAttributes): N5Grpc.Block {
    val data = UnsafeByteArrayOutputStream().use {
        DefaultBlockWriter.writeBlock(it, attributes, this)
        it.toByteArray()
        UnsafeByteOperations.unsafeWrap(it.currentBufUnsafe, 0, it.currentCount)
    }
    return N5Grpc.Block.newBuilder().setData(data).build()
}

fun N5Grpc.NullableBlock.asDataBlockOrNull(datasetAttributes: DatasetAttributes?, vararg gridPosition: Long) = when {
    hasEmpty() -> null
    else -> block.asDataBlock(datasetAttributes!!, *gridPosition)
}

fun N5Grpc.Block.asDataBlock(datasetAttributes: DatasetAttributes, vararg gridPosition: Long): DataBlock<*> =
        // TODO can we do this without asByteArray() call?
        DefaultBlockReader.readBlock(ByteArrayInputStream(data.asByteArray()), datasetAttributes, gridPosition)



private class UnsafeByteArrayOutputStream(size: Int = 32) : ByteArrayOutputStream(size) {
    val currentBufUnsafe get() = super.buf
    val currentCount get() = super.count
}
