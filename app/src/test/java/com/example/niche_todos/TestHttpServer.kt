// ABOUTME: Lightweight HTTP server helper for unit tests.
// ABOUTME: Captures a single request and responds with a fixed payload.
package com.example.niche_todos

import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class TestHttpServer private constructor(
    val port: Int,
    private val serverSocket: ServerSocket,
    private val serverThread: Thread
) : AutoCloseable {

    override fun close() {
        serverThread.join(2000)
        if (!serverSocket.isClosed) {
            serverSocket.close()
        }
    }

    companion object {
        fun start(response: String, onRequest: (String) -> Unit = {}): TestHttpServer {
            val serverSocket = ServerSocket(0)
            val serverReady = CountDownLatch(1)
            val serverThread = thread {
                serverReady.countDown()
                val socket = serverSocket.accept()
                socket.soTimeout = 2000
                val input = socket.getInputStream()
                val requestBuffer = StringBuilder()
                while (!requestBuffer.endsWith("\r\n\r\n")) {
                    val nextByte = input.read()
                    if (nextByte == -1) {
                        break
                    }
                    requestBuffer.append(nextByte.toChar())
                    if (requestBuffer.length > 8192) {
                        break
                    }
                }
                val contentLength = requestBuffer
                    .lines()
                    .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
                    ?.toIntOrNull()
                    ?: 0
                if (contentLength > 0) {
                    val bodyBytes = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val readCount = input.read(bodyBytes, readTotal, contentLength - readTotal)
                        if (readCount == -1) {
                            break
                        }
                        readTotal += readCount
                    }
                    requestBuffer.append(String(bodyBytes, 0, readTotal))
                }
                onRequest(requestBuffer.toString())
                socket.getOutputStream().use { output ->
                    output.write(response.toByteArray())
                    output.flush()
                }
                socket.close()
                serverSocket.close()
            }
            serverReady.await(2, TimeUnit.SECONDS)
            return TestHttpServer(serverSocket.localPort, serverSocket, serverThread)
        }
    }
}
