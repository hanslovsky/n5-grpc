package me.hanslovsky.n5.grpc.service

import com.google.gson.Gson
import io.grpc.ServerBuilder
import me.hanslovsky.n5.grpc.defaultGson
import org.janelia.saalfeldlab.n5.AbstractGsonReader
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.GsonAttributesParser

class N5ReaderService private constructor (private val reader: GsonAttributesParser, val gson: Gson): N5ReaderServiceBase(gson) {

    constructor(reader: GsonAttributesParser) : this(reader, if (reader is AbstractGsonReader) reader.gson else defaultGson)

    override fun readBlock(path: String, attributes: DatasetAttributes, vararg gridPosition: Long) = reader.readBlock(path, attributes, *gridPosition)
    override fun getAttributes(path: String) = reader.getAttributes(path)
    override fun getDatasetAttributs(path: String) = reader.getDatasetAttributes(path)
    override fun exists(path: String) = reader.exists(path)
    override fun datasetExists(path: String) = reader.datasetExists(path)
    override fun list(path: String) = reader.list(path)

    companion object {
        fun serve(reader: GsonAttributesParser, serverBuilder: ServerBuilder<*>) = N5GrpcServer(serverBuilder, N5ReaderService(reader))
        fun serve(reader: GsonAttributesParser, port: Int) = N5GrpcServer(port, N5ReaderService(reader))
    }

}