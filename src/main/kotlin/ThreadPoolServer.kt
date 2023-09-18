package com.example.tpserver.server

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.util.concurrent.*

class ThreadPoolServer(
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
    // Private Fields
    private var threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private var clients = ConcurrentHashMap<ClientHandler, Future<*>>()

    // Implementation methods
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
                val clientHandler = ClientHandler(socket = clientSocket)
                val clientFuture = threadPool.submit(clientHandler)
                addClient(Pair(clientHandler, clientFuture))
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

        // Shut down thread pool
        shutDownAndAwaitTermination()
    }

    /**
     * Gracefully shutdown the thread pool
     */
    private fun shutDownAndAwaitTermination() {
        threadPool.shutdown()
        try {
            // Wait a while for existing tasks to terminate
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS))
                {
                    debugPrint("Pool did not terminate")
                }
            }
        } catch (ie: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            threadPool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }

    // Private field modifiers
    /**
     *  Add client to the client list
     * @param newClient Client that will be added
     */
    @Synchronized
    private fun addClient(newClient: Pair<ClientHandler, Future<*>>)
    {
        clients[newClient.first] = newClient.second
    }

    /**
     *  Remove client from the client list
     * @param oldClient Client that will be removed
     */
    @Synchronized
    private fun removeClient(oldClient: ClientHandler)
    {
        clients.remove(oldClient)
    }


    /**
     * Handles client sockets connected to the server
     */
    private inner class ClientHandler(
        var socket: Socket
    ): Runnable {
        override fun run() {
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
                closeConnection("Could not configure client socket due to protocol error")
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
                        closeConnection("Client timed out: ${socket.inetAddress.hostName}")
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
                    closeConnection(closeMessage)
                    return
                }

            }

            // Get connection end time
            val endTime = System.currentTimeMillis()
            closeConnection("Client session successfully closed. Connection time ${endTime - startTime}")
        }

        /**
         * Gracefully close client connection
         */
        private fun closeConnection(message: String) {
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

            // Remove this client from the server's client list
            removeClient(this)
        }
    }
}