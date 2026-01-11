// ABOUTME: Unit tests for HealthCheckClient
// Validates HTTP status handling using a local test server
package com.example.niche_todos

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class HealthCheckClientTest {

    @Test
    fun fetchStatusCode_returns200ForHealthyEndpoint() = runTest {
        val response =
            "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"

        TestHttpServer.start(response).use { server ->
            val url = URL("http://127.0.0.1:${server.port}/healthz")
            val client = HealthCheckClient()

            val statusCode = client.fetchStatusCode(url)

            assertEquals(200, statusCode)
        }
    }
}
