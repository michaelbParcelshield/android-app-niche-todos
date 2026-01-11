// ABOUTME: Backend HTTP client for exchanging Google ID tokens.
// ABOUTME: Posts JSON requests and parses token responses or problem details.
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class BackendAuthResponse(
    val statusCode: Int,
    val tokens: AuthTokens?,
    val problemDetails: ProblemDetails?,
    val errorBody: String?
)

class BackendAuthClient(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun exchangeGoogleIdToken(url: URL, idToken: String): BackendAuthResponse? {
        return withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Content-Type", "application/json")
            val payload = json.encodeToString(GoogleAuthRequest.serializer(), GoogleAuthRequest(idToken))

            try {
                connection.outputStream.use { output ->
                    output.write(payload.toByteArray())
                }
                val statusCode = connection.responseCode
                val body = readBody(connection, statusCode)
                val tokens = if (statusCode in 200..299 && !body.isNullOrBlank()) {
                    json.decodeFromString(AuthTokens.serializer(), body)
                } else {
                    null
                }
                val problemDetails = if (statusCode !in 200..299 && !body.isNullOrBlank()) {
                    runCatching {
                        json.decodeFromString(ProblemDetails.serializer(), body)
                    }.getOrNull()
                } else {
                    null
                }

                BackendAuthResponse(
                    statusCode = statusCode,
                    tokens = tokens,
                    problemDetails = problemDetails,
                    errorBody = if (statusCode !in 200..299) body else null
                )
            } catch (error: IOException) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun readBody(connection: HttpURLConnection, statusCode: Int): String? {
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        return stream?.bufferedReader()?.use { it.readText() }
    }
}
