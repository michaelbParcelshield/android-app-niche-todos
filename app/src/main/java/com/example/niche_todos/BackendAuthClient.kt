// ABOUTME: Backend HTTP client for exchanging Google ID tokens.
// ABOUTME: Sends the ID token to the API and returns the response status code.
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class BackendAuthClient {
    fun exchangeGoogleIdToken(url: URL, idToken: String): Int? {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Content-Type", "application/json")
        val payload = """{"idToken":"${escapeJson(idToken)}"}"""

        return try {
            connection.outputStream.use { output ->
                output.write(payload.toByteArray())
            }
            connection.responseCode
        } catch (error: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
