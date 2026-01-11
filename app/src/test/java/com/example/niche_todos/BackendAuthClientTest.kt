// ABOUTME: Unit tests for BackendAuthClient.
// ABOUTME: Confirms Google ID tokens are POSTed to the backend endpoint.
package com.example.niche_todos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class BackendAuthClientTest {

    @Test
    fun exchangeGoogleIdToken_postsTokenAndReturnsStatus() {
        val capturedRequest = AtomicReference("")
        val response =
            "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"

        TestHttpServer.start(response) { request ->
            capturedRequest.set(request)
        }.use { server ->
            val client = BackendAuthClient()

            val status = client.exchangeGoogleIdToken(
                URL("http://127.0.0.1:${server.port}/auth/google"),
                "token-123"
            )

            assertEquals(200, status)
        }

        val request = capturedRequest.get()
        assertTrue(request.contains("POST /auth/google"))
        assertTrue(request.contains("Content-Type: application/json"))
        assertTrue(request.contains("\"idToken\":\"token-123\""))
    }
}
