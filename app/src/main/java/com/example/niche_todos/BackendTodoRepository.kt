// ABOUTME: Repository for syncing todos with the backend API.
// ABOUTME: Converts between backend payloads and in-app models using UTC normalization.
package com.example.niche_todos

import java.net.URL

class BackendTodoRepository(
    private val client: TodoClient,
    private val endpoints: BackendEndpoints,
    private val tokenStore: AuthTokenStore
) : TodoRepository {
    override suspend fun fetchTodos(): TodoSyncResult {
        val accessToken = tokenStore.load()?.accessToken
            ?: return TodoSyncResult.Failure(null, "Missing access token")

        val response = client.fetchTodos(endpoints.todosUrl, accessToken)
            ?: return TodoSyncResult.Failure(null, "Network error")

        return if (response.statusCode in 200..299 && response.body != null) {
            val todos = response.body
                .sortedBy { it.sortOrder }
                .map { payload -> payload.toTodo() }
            TodoSyncResult.Success(todos, response.statusCode)
        } else {
            TodoSyncResult.Failure(response.statusCode, response.problemDetails?.detail)
        }
    }

    override suspend fun createTodo(
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult {
        val accessToken = tokenStore.load()?.accessToken
            ?: return TodoSyncResult.Failure(null, "Missing access token")

        val request = CreateTodoRequest(
            title = title,
            startDateTimeUtc = TodoUtcConverter.toUtcString(startDateTime),
            endDateTimeUtc = TodoUtcConverter.toUtcString(endDateTime),
            isCompleted = isCompleted
        )
        val response = client.createTodo(endpoints.todosUrl, accessToken, request)
            ?: return TodoSyncResult.Failure(null, "Network error")

        if (response.statusCode !in 200..299) {
            return TodoSyncResult.Failure(response.statusCode, response.problemDetails?.detail)
        }

        return fetchTodos()
    }

    override suspend fun updateTodo(
        id: String,
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult {
        val accessToken = tokenStore.load()?.accessToken
            ?: return TodoSyncResult.Failure(null, "Missing access token")

        val request = UpdateTodoRequest(
            title = title,
            startDateTimeUtc = TodoUtcConverter.toUtcString(startDateTime),
            endDateTimeUtc = TodoUtcConverter.toUtcString(endDateTime),
            isCompleted = isCompleted
        )
        val response = client.updateTodo(todoUrlForId(id), accessToken, request)
            ?: return TodoSyncResult.Failure(null, "Network error")

        if (response.statusCode !in 200..299) {
            return TodoSyncResult.Failure(response.statusCode, response.problemDetails?.detail)
        }

        return fetchTodos()
    }

    override suspend fun deleteTodo(id: String): TodoSyncResult {
        val accessToken = tokenStore.load()?.accessToken
            ?: return TodoSyncResult.Failure(null, "Missing access token")

        val response = client.deleteTodo(todoUrlForId(id), accessToken)
            ?: return TodoSyncResult.Failure(null, "Network error")

        if (response.statusCode !in 200..299) {
            return TodoSyncResult.Failure(response.statusCode, response.problemDetails?.detail)
        }

        return fetchTodos()
    }

    override suspend fun reorderTodos(orderedIds: List<String>): TodoSyncResult {
        val accessToken = tokenStore.load()?.accessToken
            ?: return TodoSyncResult.Failure(null, "Missing access token")

        val response = client.reorderTodos(
            reorderUrl(),
            accessToken,
            ReorderTodosRequest(orderedIds)
        ) ?: return TodoSyncResult.Failure(null, "Network error")

        if (response.statusCode !in 200..299) {
            return TodoSyncResult.Failure(response.statusCode, response.problemDetails?.detail)
        }

        return fetchTodos()
    }

    private fun TodoPayload.toTodo(): Todo {
        val properties = listOf(
            TodoProperty.Title(title),
            TodoProperty.StartDateTime(TodoUtcConverter.fromUtcString(startDateTimeUtc)),
            TodoProperty.EndDateTime(TodoUtcConverter.fromUtcString(endDateTimeUtc))
        )
        return Todo(id = id, properties = properties, isCompleted = isCompleted)
    }

    private fun todoUrlForId(id: String): URL = appendPath(endpoints.todosUrl, id)

    private fun reorderUrl(): URL = appendPath(endpoints.todosUrl, "reorder")

    private fun appendPath(baseUrl: URL, suffix: String): URL {
        val trimmedBase = baseUrl.toString().trimEnd('/')
        val trimmedSuffix = suffix.trimStart('/')
        return URL("$trimmedBase/$trimmedSuffix")
    }
}
