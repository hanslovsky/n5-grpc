package org.janelia.saalfeldlab.n5.grpc.examples.server

import sun.misc.Signal
import java.util.concurrent.CountDownLatch

class SigintLog(val maxTimeSeconds: Int = 5) {

    var lastSigintLog: Long = 0L
    val maxTimeMillis = 1000 * maxTimeSeconds
    val countDownLatch = CountDownLatch(1)

    fun waitBlocking() {
        val oldHandler = Signal.handle(Signal("INT")) { handle(it) }
        countDownLatch.await()
        Signal.handle(Signal("INT"), oldHandler)
    }

    private fun handle(signal: Signal) {
        val time = System.currentTimeMillis()
        if (time - lastSigintLog < maxTimeMillis) {
            countDownLatch.countDown()
            println("\b\b")
        }
        else {
            println("\b\bReceived SIGINT signal (ctrl-c). Repeat ctrl-c within $maxTimeSeconds seconds to shutdown the server.")
            lastSigintLog = time
        }
    }

    companion object {
        fun waitBlocking() = SigintLog().waitBlocking()
    }
}