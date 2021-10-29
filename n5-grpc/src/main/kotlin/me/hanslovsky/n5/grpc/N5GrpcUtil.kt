package me.hanslovsky.n5.grpc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import me.hanslovsky.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.*
import java.nio.ByteBuffer

// TODO should we fail on null or default to "/"?
fun String?.asPath() = N5Grpc.Path.newBuilder().setPathName(this ?: "/").build()
fun N5Grpc.JsonString.reader() = jsonString.reader()
fun ByteBuffer.asByteArray() = ByteArray(capacity()) { this[it] }
fun ByteString.asByteArray() = asReadOnlyByteBuffer().asByteArray()

fun DatasetAttributes.asMessage(gson: Gson = defaultGson): N5Grpc.DatasetAttributes {
    return N5Grpc.DatasetAttributes.newBuilder().also { b ->
        dimensions.forEach { b.addDimensions(it) }
        blockSize.forEach { b.addBlockSize(it) }
        b.dataType = dataType.toString()
        b.compressionJsonString = compression.toJson(gson)
    }.build()
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