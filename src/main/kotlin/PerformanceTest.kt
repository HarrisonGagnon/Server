package com.example.tpserver.server

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import kotlin.concurrent.thread

/**
 * Compare performance of different server implementations
 * @param serversAddresses Address of servers to
 */
fun testServers(serversAddresses: Array<SocketAddress>){

    serversAddresses.forEach {
        println("Server: $it")
        testServer(it)
    }
}

/**
 * Test performance of given server
 * @param serverAddress Address of the server to test
 * @param cliPortStart Start of range of 1000 ports that will be used during testing
 */
private fun testServer(serverAddress: SocketAddress, cliPortStart: Int = 9000) {
    // Create dummy clients to connect to the server
    val times = Array(1000) { 0L }
    for (i in 0..999)
    {
        times[i] = 0
        thread {
            try {
                val client = Socket()
                client.bind(InetSocketAddress(i+cliPortStart))
                client.connect(serverAddress)
                times[i] = System.nanoTime()
                if (client.getInputStream().read() == 0x01)
                {
                    times[i] = System.nanoTime() - times[i]
                }
                client.close()
            }
            catch (e: Exception) {
//                println(e.message)
            }
        }
    }

    // Calculate server performance
    var unconnectedClients = 0
    var minTime = Long.MAX_VALUE
    var maxTime = Long.MIN_VALUE
    for (time in times) {
        if (time == 0L) { unconnectedClients++}
        else {
            if (time > maxTime) { maxTime = time }
            if (time < minTime) { minTime = time }
        }
    }
    // Output server performance
    println("Min time to connect $minTime")
    println("Max time to connect $maxTime")
    println("$unconnectedClients clients could not connect")
}