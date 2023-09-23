package server

import java.io.IOException
import java.net.*
import java.util.concurrent.*

class ThreadPoolServer(
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
    private var threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private var clients = ConcurrentHashMap<ClientHandler, Future<*>>()

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
    private fun removeClient(oldClient: Pair<ClientHandler, Future<*>?>)
    {
        if (oldClient.second?.isDone == false) {
            debugPrint("Removing client that did not finish! Client: ${oldClient.first.socket.inetAddress.hostName}")
        }

        clients.remove(oldClient.first)
    }


    /**
     * Handles client sockets connected to the server
     */
    private inner class ClientHandler(
        socket: Socket
    ): Runnable {
        // Private fields
        val connection = ClientConnection(socket)

        // Public fields
        val socket = socket

        override fun run() {
            // Create client connection
            val connection = ClientConnection(socket)

            // Determine if connection was established
            if (!connection.getOpened()) {
                closeConnection("Could not establish client socket I/O streams")
                return
            }

            // Configure socket
            try {
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
                        connection.output!!.write("HTTP/1.1 200 OK\n\n Hello client!".toByteArray())
                        helloSent = true
                    }

                    // Read socket input byte by byte
                    val byte = connection.input!!.read()
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

            // Close connection then remove this client from the server's client list
            closeConnection("Client session successfully closed.")
        }

        /**
         * Close client connection then remove client from client list
         */
        private fun closeConnection(message: String) {

            connection.close()
            debugPrint(message)
            removeClient(Pair(this, clients[this]))
        }
    }
}