// ABOUTME: Unit tests for BackendAuthClient.
// ABOUTME: Confirms Google ID tokens are POSTed to the backend endpoint.
package com.example.niche_todos

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class BackendAuthClientTest {

    @Test
    fun exchangeGoogleIdToken_postsTokenAndParsesResponse() = runTest {
        val capturedRequest = AtomicReference("")
        val userId = "8a8c1a60-126c-49ee-a76f-3c7ad7085b2a"
        val payload = """
            {
              "accessToken": "access-123",
              "expiresInSeconds": 3600,
              "refreshToken": "refresh-456",
              "user": {
                "id": "$userId",
                "email": "alex@example.com",
                "name": "Alex Example",
                "avatarUrl": null
              }
            }
        """.trimIndent()
        val response =
            "HTTP/1.1 200 OK\r\nContent-Length: ${payload.toByteArray().size}\r\n" +
                "Connection: close\r\n\r\n$payload"

        TestHttpServer.start(response) { request ->
            capturedRequest.set(request)
        }.use { server ->
            val client = BackendAuthClient()

            val backendResponse = client.exchangeGoogleIdToken(
                URL("http://127.0.0.1:${server.port}/auth/google"),
                "token-123"
            )

            assertNotNull(backendResponse)
            assertEquals(200, backendResponse?.statusCode)
            assertEquals("access-123", backendResponse?.tokens?.accessToken)
            assertEquals(3600, backendResponse?.tokens?.expiresInSeconds)
            assertEquals("refresh-456", backendResponse?.tokens?.refreshToken)
            assertEquals(userId, backendResponse?.tokens?.user?.id)
        }

        val request = capturedRequest.get()
        assertTrue(request.contains("POST /auth/google"))
        assertTrue(request.contains("Content-Type: application/json"))
        assertTrue(request.contains("\"idToken\":\"token-123\""))
    }
}
