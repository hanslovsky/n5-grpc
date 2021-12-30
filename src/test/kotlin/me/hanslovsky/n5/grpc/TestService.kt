package me.hanslovsky.n5.grpc

import com.google.gson.JsonElement
import io.grpc.stub.StreamObserver
import me.hanslovsky.n5.grpc.service.N5ReaderServiceBase
import org.janelia.saalfeldlab.n5.DataBlock
import org.janelia.saalfeldlab.n5.DatasetAttributes

class TestService(
    var readBlock: ((String, DatasetAttributes, LongArray) -> DataBlock<*>?)? = null,
    var getAttributes: ((String) -> Map<String, JsonElement>)? = null,
    var getDatasetAttributes: ((String) -> DatasetAttributes?)? = null,
    var exists: ((String) -> Boolean)? = null,
    var list: ((String) -> Array<String>)? = null,
    var datasetExists: ((String) -> Boolean)? = null
) : N5ReaderServiceBase(defaultGson) {

    override fun readBlock(path: String, attributes: DatasetAttributes, vararg gridPosition: Long) = readBlock.let {
        if(it == null) error("Implementation for readBlock not provided")
        it(path, attributes, gridPosition)
    }

    override fun getAttributes(path: String): Map<String, JsonElement> = getAttributes.let {
        if (it == null) error("Implementation for getAttributes not provided")
        it(path)
    }

    override fun getDatasetAttributes(path: String) = getDatasetAttributes.let {
        if (it == null) error ("Implementation for getDatasetAttributes not provided")
        it(path)
    }

    override fun exists(path: String) = exists.let {
        if (it == null) error ("Implementation for exists not provided")
        it(path)
    }

    override fun datasetExists(path: String) = datasetExists.let {
        if (it == null) error("Implementation for datasetExists not provided")
        it(path)
    }

    override fun list(path: String) = list.let {
        if (it == null) error("Implementation for list not provided")
        it(path)
    }
}

fun <T> StreamObserver<T>.onNextAndOnCompleted(data: T) {
    onNext(data)
    onCompleted()
}