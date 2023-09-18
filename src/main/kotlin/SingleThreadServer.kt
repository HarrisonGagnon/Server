package com.example.tpserver.server

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*

class SingleThreadServer(
    port: Int = 6161,
    address: InetAddress? = null,
    isQuiet: Boolean
):
// Extends
    Server(
        port = port,
        address = address,
        isQuiet = isQuiet
    )
{
    // Interface implementation methods
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
        // Get and Set socket fields
        val input: InputStream
        val output: OutputStream
        try {
            // Get socket I/O
            input = socket.getInputStream()
            output = socket.getOutputStream()

            // Set socket configuration
            socket.soTimeout = 1000 * 5 // timeout in milliseconds
        }
        catch (e: SocketException) {
            closeConnection(socket, "Could not configure client socket due to protocol error")
            return
        }

        // Get connection start time
        val startTime = System.currentTimeMillis()
        debugPrint("Client connected: $startTime")
        debugPrint("IP Address: ${socket.inetAddress.hostName}")

       // Main client logic
        while (!socket.isClosed) {
            try {
                // Check for disconnection
                output.write(0)
                output.flush()

                // Read socket input byte by byte
                val byte = input.read()
                if (byte == -1) {
                    closeConnection(socket, "Client timed out: ${socket.inetAddress.hostName}")
                    return
                }
                else
                {
                    // TODO: Handle clients
                    socket.getOutputStream().write(0x01)

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
                closeConnection(socket, closeMessage)
                return
            }

        }

        // Get connection end time
        val endTime = System.currentTimeMillis()
        closeConnection(socket, "Client session successfully closed. Connection time ${endTime - startTime}")
    }

    /**
     * Gracefully close client connection
     */
    private fun closeConnection(socket:Socket, message: String) {
        try {
            // Close socket IO streams
            socket.getInputStream().close()
            socket.getOutputStream().close()


            // Close socket and print reason for closing
            socket.close()
        }
        catch (e: IOException) {
            debugPrint(e.message.toString())
        }

        // Print why the socket is being closed
        debugPrint(message)
    }
}