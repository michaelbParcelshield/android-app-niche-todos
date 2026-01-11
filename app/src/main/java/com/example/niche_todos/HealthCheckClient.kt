// ABOUTME: Minimal HTTP client for health check endpoints.
// ABOUTME: Provides a suspend method that returns the response status code.
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface HealthClient {
    suspend fun fetchStatusCode(url: URL): Int?
}

class HealthCheckClient(
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
) : HealthClient {
    override suspend fun fetchStatusCode(url: URL): Int? {
        return withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs

            try {
                connection.responseCode
            } catch (error: IOException) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    private companion object {
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 5000
        private const val DEFAULT_READ_TIMEOUT_MS = 5000
    }
}
