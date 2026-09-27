package com.egor201.puffy.core

// TCP connect-time latency probe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    suspend fun pingMs(host: String, port: Int, timeoutMs: Int = 2500): Int? = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val elapsed = (System.currentTimeMillis() - start).toInt()
            socket.close()
            elapsed
        } catch (e: Exception) {
            null
        }
    }
}
