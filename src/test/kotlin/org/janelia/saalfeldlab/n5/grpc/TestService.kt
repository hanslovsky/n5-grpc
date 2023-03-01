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
import io.grpc.stub.StreamObserver
import org.janelia.saalfeldlab.n5.DataBlock
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.grpc.service.N5ReaderServiceBase

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
