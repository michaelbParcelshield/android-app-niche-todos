// ABOUTME: Backend HTTP client for todo CRUD and ordering operations.
// ABOUTME: Sends JSON with bearer auth and parses todo responses.
package com.example.niche_todos

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class BackendTodoResponse<T>(
    val statusCode: Int,
    val body: T?,
    val problemDetails: ProblemDetails?,
    val errorBody: String?
)

interface TodoClient {
    suspend fun fetchTodos(url: URL, accessToken: String): BackendTodoResponse<List<TodoPayload>>?
    suspend fun createTodo(
        url: URL,
        accessToken: String,
        request: CreateTodoRequest
    ): BackendTodoResponse<TodoPayload>?
    suspend fun updateTodo(
        url: URL,
        accessToken: String,
        request: UpdateTodoRequest
    ): BackendTodoResponse<TodoPayload>?
    suspend fun deleteTodo(url: URL, accessToken: String): BackendTodoResponse<Unit>?
    suspend fun reorderTodos(
        url: URL,
        accessToken: String,
        request: ReorderTodosRequest
    ): BackendTodoResponse<Unit>?
}

class BackendTodoClient(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS
) : TodoClient {
    override suspend fun fetchTodos(
        url: URL,
        accessToken: String
    ): BackendTodoResponse<List<TodoPayload>>? {
        return executeRequest(
            url = url,
            method = "GET",
            accessToken = accessToken,
            payload = null
        ) { body ->
            json.decodeFromString(ListSerializer(TodoPayload.serializer()), body)
        }
    }

    override suspend fun createTodo(
        url: URL,
        accessToken: String,
        request: CreateTodoRequest
    ): BackendTodoResponse<TodoPayload>? {
        val payload = json.encodeToString(CreateTodoRequest.serializer(), request)
        return executeRequest(
            url = url,
            method = "POST",
            accessToken = accessToken,
            payload = payload
        ) { body ->
            json.decodeFromString(TodoPayload.serializer(), body)
        }
    }

    override suspend fun updateTodo(
        url: URL,
        accessToken: String,
        request: UpdateTodoRequest
    ): BackendTodoResponse<TodoPayload>? {
        val payload = json.encodeToString(UpdateTodoRequest.serializer(), request)
        return executeRequest(
            url = url,
            method = "PUT",
            accessToken = accessToken,
            payload = payload
        ) { body ->
            json.decodeFromString(TodoPayload.serializer(), body)
        }
    }

    override suspend fun deleteTodo(url: URL, accessToken: String): BackendTodoResponse<Unit>? {
        return executeRequest(
            url = url,
            method = "DELETE",
            accessToken = accessToken,
            payload = null
        ) { Unit }
    }

    override suspend fun reorderTodos(
        url: URL,
        accessToken: String,
        request: ReorderTodosRequest
    ): BackendTodoResponse<Unit>? {
        val payload = json.encodeToString(ReorderTodosRequest.serializer(), request)
        return executeRequest(
            url = url,
            method = "PUT",
            accessToken = accessToken,
            payload = payload
        ) { Unit }
    }

    private suspend fun <T> executeRequest(
        url: URL,
        method: String,
        accessToken: String,
        payload: String?,
        parser: (String) -> T
    ): BackendTodoResponse<T>? {
        return withContext(Dispatchers.IO) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
            }

            try {
                if (payload != null) {
                    connection.outputStream.use { output ->
                        output.write(payload.toByteArray())
                    }
                }
                val statusCode = connection.responseCode
                val body = readBody(connection, statusCode)
                val parsedBody = parseBody(body, parser)
                val problemDetails = if (statusCode !in 200..299 && !body.isNullOrBlank()) {
                    runCatching {
                        json.decodeFromString(ProblemDetails.serializer(), body)
                    }.getOrNull()
                } else {
                    null
                }

                BackendTodoResponse(
                    statusCode = statusCode,
                    body = if (statusCode in 200..299) parsedBody else null,
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

    private fun <T> parseBody(body: String?, parser: (String) -> T): T? {
        if (body.isNullOrBlank()) {
            return null
        }
        return runCatching { parser(body) }.getOrNull()
    }

    private fun readBody(connection: HttpURLConnection, statusCode: Int): String? {
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        return stream?.bufferedReader()?.use { it.readText() }
    }

    private companion object {
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 5000
        private const val DEFAULT_READ_TIMEOUT_MS = 5000
    }
}
