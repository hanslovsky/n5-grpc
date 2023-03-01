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
package org.janelia.saalfeldlab.n5.grpc.examples.client

import bdv.util.BdvFunctions
import bdv.util.BdvOptions
import bdv.util.volatiles.SharedQueue
import bdv.util.volatiles.VolatileViews
import net.imglib2.RandomAccessibleInterval
import net.imglib2.type.numeric.integer.*
import net.imglib2.type.numeric.real.DoubleType
import net.imglib2.type.numeric.real.FloatType
import org.janelia.saalfeldlab.n5.DataType
import org.janelia.saalfeldlab.n5.N5Reader
import org.janelia.saalfeldlab.n5.grpc.N5GrpcReader
import org.janelia.saalfeldlab.n5.grpc.generated.N5Grpc
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "N5 GRPC Client BDV", showDefaultValues = true)
class VisualizeWithBdv: Callable<Int> {


    @CommandLine.Option(names = ["--host"], required = false, defaultValue = "localhost")
    lateinit var host: String

    @CommandLine.Option(names = ["--port", "-p"], required = false, defaultValue = "9090")
    var port: Int = 9090

    @CommandLine.Option(names = ["--dataset", "-d"], required = false)
    lateinit var datasets: Array<String>

    @CommandLine.Option(names = ["--num-fetcher-threads", "-n"], required = false, defaultValue = "1")
    var numFetcherThreads: Int = 1

    @CommandLine.Option(names = ["--min", "-m"], required = false, defaultValue = "0")
    var min: Double = 0.0

    @CommandLine.Option(names = ["--max", "-M"], required = false, defaultValue = "4096")
    var max: Double = 4096.0

    @CommandLine.Option(names = ["--help", "-h"], required = false, usageHelp = true)
    var help: Boolean = false

    @CommandLine.Option(names = ["--health-check"], required = false)
    var healthCheck: Boolean = false

    override fun call(): Int {

        if (healthCheck) {
            N5GrpcReader.AutoCloseableReader(host, port).use {
                val healthStatus = it.healthCheck()
                println("healthStatus=$healthStatus")
                return when(healthStatus) {
                    N5Grpc.HealthStatus.Status.SERVING -> 0
                    else -> 1
                }
            }
        }

        if (!this::datasets.isInitialized) {
            System.err.println("No datasets provided.")
            return 1
        }

        val queue = SharedQueue(numFetcherThreads)
        val reader = N5GrpcReader(host, port)
        val data = datasets.map { it to reader.open(it, queue) }
        val bdv = data.firstOrNull()
            ?.let { (n, rai) -> BdvFunctions.show(rai, n) }
            ?.let { data.subList(1, data.size).fold(it) { bdv, (n, rai) -> BdvFunctions.show(rai, n, BdvOptions.options().addTo(bdv)) } }
            ?: error("No datasets provided")
        bdv.converterSetups.forEach { it.setDisplayRange(min, max) }
        while (bdv.bdvHandle.viewerPanel.isShowing) {
            Thread.sleep(10)
        }
        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(VisualizeWithBdv()).execute(*args))
        }
    }
}


private fun N5Reader.open(dataset: String, queue: SharedQueue? = null): RandomAccessibleInterval<*> {
    require(datasetExists(dataset)) {"Dataset $dataset does not exist"}
    val attributes = getDatasetAttributes(dataset)
    val rai = when (attributes.dataType) {
        DataType.INT8 -> N5Utils.openVolatile<ByteType>(this, dataset)
        DataType.INT16 -> N5Utils.openVolatile<ShortType>(this, dataset)
        DataType.INT32 -> N5Utils.openVolatile<IntType>(this, dataset)
        DataType.INT64 -> N5Utils.openVolatile<LongType>(this, dataset)
        DataType.UINT8 -> N5Utils.openVolatile<UnsignedByteType>(this, dataset)
        DataType.UINT16 -> N5Utils.openVolatile<UnsignedShortType>(this, dataset)
        DataType.UINT32 -> N5Utils.openVolatile<UnsignedIntType>(this, dataset)
        DataType.UINT64 -> N5Utils.openVolatile<UnsignedLongType>(this, dataset)
        DataType.FLOAT32 -> N5Utils.openVolatile<FloatType>(this, dataset)
        DataType.FLOAT64 -> N5Utils.openVolatile<DoubleType>(this, dataset)
        else -> error("Unsupported datatype: ${attributes.dataType}")
    }
    return VolatileViews.wrapAsVolatile(rai, queue)
}
