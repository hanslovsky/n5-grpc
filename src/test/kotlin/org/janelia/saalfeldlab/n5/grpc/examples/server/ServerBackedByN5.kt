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
package org.janelia.saalfeldlab.n5.grpc.examples.server

import com.google.gson.Gson
import org.janelia.saalfeldlab.n5.GsonAttributesParser
import org.janelia.saalfeldlab.n5.grpc.N5GrpcReader
import org.janelia.saalfeldlab.n5.grpc.service.N5ReaderService
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader
import picocli.CommandLine
import java.util.concurrent.Callable


class ServerBackedByN5: Callable<Int> {

    @CommandLine.Option(names = ["--hdf5-file", "-f"], required = true)
    private lateinit var hdf5Path: String

    @CommandLine.Option(names =["--default-block-size", "-b"], required=false, defaultValue = "16", split = ",")
    private lateinit var defaultBlockSize: IntArray

    @CommandLine.Option(names = ["--port", "-p"], required=false, defaultValue = defaultPort)
    private var port: Int = defaultPort.toInt()

    val defaultBlockSize3D get() = defaultBlockSize
        .also { require(it.size in setOf(1, 3)) {"Default block size can only be specified as 1D or 3D but got ${it.joinToString()}"} }
        .takeIf { it.size ==3 } ?: IntArray(3) { defaultBlockSize[0] }

    override fun call(): Int {
        val h5Reader = N5HDF5Reader(
            hdf5Path,
            true,
            *defaultBlockSize3D)
        val server = N5ReaderService.serve(h5Reader as GsonAttributesParser, 9090)
        server.start()
        val reader = N5GrpcReader("localhost", 9090)
        println("Contents of hdf file $hdf5Path (datasets highlighted by *)")
        val stack = mutableListOf("/")
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val isDataset = reader.datasetExists(current)
            println("$current${if (isDataset) "*" else ""}")
            if (!isDataset)
                stack.addAll(reader.list(current).map { "${if (current == "/") "" else current}/$it" })
        }
        SigintLog.waitBlocking()
        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine(ServerBackedByN5()).execute(*args)
        }

        private const val defaultPort = "9090"
    }
}
