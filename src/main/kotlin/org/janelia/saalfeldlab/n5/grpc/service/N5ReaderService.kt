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

import com.google.gson.Gson
import io.grpc.ServerBuilder
import org.janelia.saalfeldlab.n5.grpc.defaultGson
import org.janelia.saalfeldlab.n5.AbstractGsonReader
import org.janelia.saalfeldlab.n5.DatasetAttributes
import org.janelia.saalfeldlab.n5.GsonAttributesParser

class N5ReaderService private constructor (private val reader: GsonAttributesParser, gson: Gson): N5ReaderServiceBase(gson) {

    constructor(reader: GsonAttributesParser) : this(reader, if (reader is AbstractGsonReader) reader.gson else defaultGson)

    override fun readBlock(path: String, attributes: DatasetAttributes, vararg gridPosition: Long) = reader.readBlock(path, attributes, *gridPosition)
    override fun getAttributes(path: String) = reader.getAttributes(path)
    override fun getDatasetAttributes(path: String) = reader.getDatasetAttributes(path)
    override fun exists(path: String) = reader.exists(path)
    override fun datasetExists(path: String) = reader.datasetExists(path)
    override fun list(path: String) = reader.list(path)

    companion object {
        fun serve(reader: GsonAttributesParser, serverBuilder: ServerBuilder<*>) = N5GrpcServer(serverBuilder, N5ReaderService(reader))
        fun serve(reader: GsonAttributesParser, port: Int) = N5GrpcServer(port, N5ReaderService(reader))
    }

}
