package com.example.tpserver.server

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketAddress

/**
 * Server interface
 */
abstract class Server(
    port: Int = 6161,
    address: InetAddress? = null,
    isQuiet: Boolean
):
    // Implements
    Runnable
{
    // Constants
    protected val debugPrint = { message: String ->
        if(!this.isQuiet) {
            println(message)
        }
    }

    // Private fields
    private val port: Int
    private val address: InetAddress?
    private var isStopped = false
    private var isQuiet = true

    // Protected fields
    protected var serverThread: Thread = Thread.currentThread()
    protected lateinit var serverSocket: ServerSocket
//    protected val protocol

    // Constructor
    init {
        this.port = port
        this.address = address
        this.isQuiet = isQuiet

        openServerSocket()
    }

    // Public server functions
    /**
     * Check if server is stopped
     * @return true if the server is stopped and false otherwise
     */
    @Synchronized
    fun isStopped() = isStopped

    /**
     * Stop server functions
     * @throws RuntimeException if I/O error occurs when closing the server socket
     */
    @Synchronized
    fun stop() {

        isStopped = true
        serverSocket.close()

        debugPrint("Server Stopped.")
    }

    /**
     * Get server socket address
     * @throws RuntimeException if the server is stopped
     */
    @Synchronized
    fun getServerSocketAddress(): SocketAddress {
        if (!isStopped)
        {
            return serverSocket.localSocketAddress
        }
        else {
            throw RuntimeException("Server is stopped")
        }
    }

    // Private server functions
    @Synchronized
    /**
     * Open server socket on the specified port
     * @throws IOException if I/O error occurs when opening the socket
     */
    private fun openServerSocket() {
        serverSocket = if (address != null) {
            ServerSocket(port, 50, address)
        } else {
            ServerSocket(port)
        }
        debugPrint("Server opened at ${serverSocket.localSocketAddress}, looking for clients")
    }
}