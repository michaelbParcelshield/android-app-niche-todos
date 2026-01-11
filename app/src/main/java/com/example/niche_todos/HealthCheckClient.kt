// ABOUTME: Minimal HTTP client for health check endpoints.
// ABOUTME: Provides a suspend method that returns the response status code.
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthCheckClient {
    suspend fun fetchStatusCode(url: URL): Int? {
        return withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                connection.responseCode
            } catch (error: IOException) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }
}
