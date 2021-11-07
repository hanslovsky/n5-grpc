package me.hanslovsky.n5.grpc.server

import bdv.util.BdvFunctions
import com.google.gson.JsonElement
import io.grpc.ServerBuilder
import me.hanslovsky.n5.grpc.defaultGson
import me.hanslovsky.n5.grpc.service.N5ReaderServiceBase
import net.imglib2.RandomAccessibleInterval as RAI
import net.imglib2.img.array.ArrayImgs
import net.imglib2.position.FunctionRandomAccessible
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedLongType
import net.imglib2.view.Views
import org.janelia.saalfeldlab.n5.*
import java.awt.Color
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil


class RasterizedTextServer(
    private val blockSize: IntArray = intArrayOf(32, 32, 32),
    vararg val text: String) : N5ReaderServiceBase(defaultGson) {

    private val image = rasterize(defaultFont, 1, *text)
    private val size = blockSize.fold(1) { prod, el -> prod * el }
    private val dimensions = blockSize.mapIndexed { d, bs -> bs * image.dimension(d) }.toLongArray()

    override fun readBlock(path: String, attributes: DatasetAttributes, vararg gridPosition: Long): DataBlock<*>? {
        val data = image.randomAccess().setPositionAndGet(*gridPosition).get()
        if (data == 0)
            return null
        return when (path) {
            "uint8" -> ByteArrayDataBlock(blockSize, gridPosition, ByteArray(size) { data.toByte() })
            else -> null
        }
    }

    override fun getAttributes(path: String): Map<String, JsonElement> = when {
        path.trim('/').let { p -> DataType.values().any { it.toString() == p } } -> mapOf("text" to gson.toJsonTree(text))
        else -> mapOf()
    }

    override fun getDatasetAttributes(path: String): DatasetAttributes {
        val dt = DataType.fromString(path)
        return DatasetAttributes(dimensions, blockSize, dt, GzipCompression())
    }

    override fun exists(path: String): Boolean {
        return when (path.trim()) {
            "", "uint8" -> true
            else -> false
        }
    }

    override fun datasetExists(path: String) = path.trim() == "uint8"

    override fun list(path: String): Array<String> = when(path.trim('/')) {
        "" -> DataType.values().map { it.toString() }.toTypedArray()
        else -> arrayOf()
    }

    companion object {
        private val defaultFont = Font( "Monospaced", Font.PLAIN, 12 )
        private val Rectangle2D.intWidth get() = ceil(width).toInt()
        private val Rectangle2D.intHeight get() = ceil(height).toInt()

        private fun rasterize(font: Font = defaultFont, spacing: Int = 1, vararg text: String): RAI<UnsignedByteType> {
            val pages = text.map { it.split("\n") }.toTypedArray()
            val numLines = pages.map { it.size }
            val bounds = pages.map { it.map { it.estimateBounds(font) } }
            val maxWidth = bounds.map { it.map { it.intWidth }.maxOrNull() ?: 0 }.maxOrNull() ?: 0
            val maxLineHeight = bounds.map { it.map { it.intHeight }.maxOrNull() ?: 0 }.maxOrNull() ?: 0
            val maxPageHeight = numLines.maxOrNull()?.takeIf { it > 0 }?.let { it * (spacing + maxLineHeight) - spacing } ?: 0
            val imgs = pages.map { it.rasterize(maxWidth, maxPageHeight, spacing + maxLineHeight, font) }
            return Views.stack(imgs)
        }

        private fun String.estimateBounds(font: Font): Rectangle2D {
            val context = FontRenderContext(null, true, true)
            return font.getStringBounds(this, context)
        }

        private fun Iterable<String>.rasterize(
            width: Int,
            height: Int,
            stride: Int,
            font: Font
        ): RAI<UnsignedByteType> {
            val bi = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            val g = bi.createGraphics()
            g.font = font
            g.color = Color.WHITE
            for ((idx, line) in this.withIndex())
                g.drawString(line, 0, idx * stride + font.size)
            val pixels = (bi.raster.dataBuffer as DataBufferByte).data
            return ArrayImgs.unsignedBytes(pixels, width.toLong(), height.toLong());
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val threadId = AtomicInteger()
            val port = 9090
            val numThreads = 3
            val server = ServerBuilder
                .forPort(port)
                .executor(Executors.newFixedThreadPool(numThreads) { Thread(it).also { it.isDaemon = true; it.name = "n5-grpc-server-${threadId.getAndIncrement()}" } })
                .addService(RasterizedTextServer(intArrayOf(32, 32, 32), "N5 is", "", "the", "best\n\there\nis", "", "especially\nwith", "Kotlin"))
                .build()
            server.start()

            SigintLog.waitBlocking()

        }
    }
}