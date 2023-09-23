package server

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress

/**
 * Server abstraction
 */
abstract class Server(
    port: Int = 6161,
    address: InetAddress? = null,
    isQuiet: Boolean = true
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
    private val port = port
    private val address = address
    private var isStopped = false

    // Protected fields
    protected var serverThread: Thread = Thread.currentThread()
    protected lateinit var serverSocket: ServerSocket

    // Public fields
    var isQuiet = isQuiet

    // Constructor
    init {
        // Attempt to open server socket
        openServerSocket()
    }

    protected inner class ClientConnection(
        socket: Socket
    ):
        // Implements
        Closeable, AutoCloseable
    {
        // Private
        private var opened: Boolean

        // Public fields
        val startTime: Long = System.currentTimeMillis()
        val socket: Socket
        var input: InputStream?
        var output: OutputStream?

        // TODO: Add socket configuration options

        init {
            this.socket = socket
            try {
                input = this.socket.getInputStream()
                output = this.socket.getOutputStream()
                opened = true
            }
            catch (e: IOException) {
                input = null
                output = null
                opened = false
            }
        }

        fun getOpened() = opened

        override fun close() {
            try {
                // Close socket IO streams
                input?.close()
                output?.close()

                // Close socket and print reason for closing
                socket.close()
            }
            catch (e: IOException) {
                debugPrint(e.message.toString())
            }
            // Record time of connection termination
            val endTime = System.currentTimeMillis()

            // Print connection time and client IP
            debugPrint("Connection time: ${endTime - startTime}")
        }
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
        try {
            // Open server socket on specified port/address
            serverSocket = if (address != null) {
                ServerSocket(port, 50, address)
            } else {
                ServerSocket(port)
            }

            debugPrint("Server opened at ${serverSocket.localSocketAddress}, looking for clients")
        }
        catch (e: Exception) {
            e.message?.let { debugPrint(it) }
            debugPrint("Could not open server socket! Server shutting down")
            stop()
        }
    }
}