// ABOUTME: Minimal HTTP client for health check endpoints
// ABOUTME: Provides a blocking method that returns the response status code
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HealthCheckClient {
    fun fetchStatusCode(url: URL): Int? {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        return try {
            connection.responseCode
        } catch (error: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
