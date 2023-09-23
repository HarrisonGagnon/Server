package server

import java.io.IOException
import java.net.*

class SingleThreadServer(
    port: Int = 6161,
    address: InetAddress? = null,
    isQuiet: Boolean = true
):
    // Extends
    Server(
        port = port,
        address = address,
        isQuiet = isQuiet
    )
{
    // Private fields
    private lateinit var clientConnection: ClientConnection

    // Abstraction implementations
    /**
     *  Server thread that opens server socket on specified port
     *  and listens for clients
     */
    override fun run() {

        // Look for clients until stopped
        while (!isStopped()) {

            var clientSocket: Socket
            try {
                // Listen for new clients
                clientSocket = serverSocket.accept()

                // Submit new client work to thread pool
                processClientRequest(clientSocket)
            }
            catch (e: Exception) {

                e.message?.let { debugPrint(it) }
                if (isStopped()) {
                    debugPrint("Server is stopped and cannot listen for clients")
                }
                else {
                    throw RuntimeException("Error accepting client connection", e)
                }
            }
        }
    }

    private fun processClientRequest(socket: Socket) {
        // Set client connection
        clientConnection = ClientConnection(socket)

        // Configure client socket
        try {
            // Set socket configuration
            socket.soTimeout = 1000 * 5 // timeout in milliseconds
        }
        catch (e: SocketException) {
            closeConnection("Could not configure client socket due to protocol error")
            return
        }

        // Main client logic
        var helloSent = false
        while (!socket.isClosed) {
            try {
                if (!helloSent) {
                    // Server hello
                    clientConnection.output!!.write("HTTP/1.1 200 OK\n\n Hello client!".toByteArray())
                    helloSent = true
                }

                // Read socket input byte by byte
                val byte = clientConnection.input!!.read()
                if (byte == -1) {
                    closeConnection("Client timed out")
                    return
                }
                else
                {
                    // TODO: Handle clients

                    debugPrint("Received $byte")
                }
            }
            catch (e: Exception) {
                // Set exception message
                val closeMessage = when (e.javaClass) {
                    IOException::class.java -> {
                        "Client IO Exception: ${socket.inetAddress.hostName}"
                    }

                    SocketTimeoutException::class.java -> {
                        "Client socket timed out: ${socket.inetAddress.hostName}"
                    }

                    else -> {
                        e.message.toString()
                    }
                }
                // Close client connection
                closeConnection(closeMessage)
                return
            }
        }

        // Get connection end time
        closeConnection("Client session successfully closed")
    }

    /**
     * Gracefully close client connection
     */
    private fun closeConnection(message: String) {

        clientConnection.close()
        debugPrint(message)
    }
}