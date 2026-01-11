// ABOUTME: Test doubles for backend repositories.
// ABOUTME: Lets tests control backend responses deterministically.
package com.example.niche_todos

class FakeHealthRepository(
    private val result: HealthCheckResult
) : HealthRepository {
    override suspend fun runHealthCheck(): HealthCheckResult = result
}

class FakeHealthClient(
    private val statusCode: Int?
) : HealthClient {
    override suspend fun fetchStatusCode(url: java.net.URL): Int? = statusCode
}

class FakeAuthRepository(
    private val result: AuthResult = AuthResult.Success(
        AuthTokens(
            accessToken = "access",
            expiresInSeconds = 3600,
            refreshToken = "refresh",
            user = AuthenticatedUser(
                id = "user-id",
                email = "test@example.com",
                name = "Test User",
                avatarUrl = null
            )
        ),
        statusCode = 200
    )
) : AuthRepository {
    override suspend fun exchangeGoogleIdToken(idToken: String): AuthResult = result
}

class FakeBackendAuthClient(
    private val response: BackendAuthResponse?
) : AuthClient {
    override suspend fun exchangeGoogleIdToken(url: java.net.URL, idToken: String): BackendAuthResponse? {
        return response
    }
}

class FakeAuthTokenStore : AuthTokenStore {
    var savedTokens: AuthTokens? = null

    override fun save(tokens: AuthTokens) {
        savedTokens = tokens
    }

    override fun load(): AuthTokens? = savedTokens

    override fun clear() {
        savedTokens = null
    }
}

class FakeTodoRepository(
    initialTodos: List<Todo> = emptyList(),
    private val idProvider: () -> String = { java.util.UUID.randomUUID().toString() },
    private val shouldFail: Boolean = false
) : TodoRepository {
    private val todos = initialTodos.toMutableList()

    override suspend fun fetchTodos(): TodoSyncResult = respondWithTodos()

    override suspend fun createTodo(
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult {
        if (shouldFail) {
            return TodoSyncResult.Failure(500, "Failure")
        }

        todos.add(
            Todo(
                id = idProvider(),
                properties = listOf(
                    TodoProperty.Title(title),
                    TodoProperty.StartDateTime(startDateTime),
                    TodoProperty.EndDateTime(endDateTime)
                ),
                isCompleted = isCompleted
            )
        )
        return respondWithTodos()
    }

    override suspend fun updateTodo(
        id: String,
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult {
        if (shouldFail) {
            return TodoSyncResult.Failure(500, "Failure")
        }

        val index = todos.indexOfFirst { it.id == id }
        if (index == -1) {
            return TodoSyncResult.Failure(404, "Not found")
        }

        todos[index] = todos[index].copy(
            properties = listOf(
                TodoProperty.Title(title),
                TodoProperty.StartDateTime(startDateTime),
                TodoProperty.EndDateTime(endDateTime)
            ),
            isCompleted = isCompleted
        )
        return respondWithTodos()
    }

    override suspend fun deleteTodo(id: String): TodoSyncResult {
        if (shouldFail) {
            return TodoSyncResult.Failure(500, "Failure")
        }

        todos.removeAll { it.id == id }
        return respondWithTodos()
    }

    override suspend fun reorderTodos(orderedIds: List<String>): TodoSyncResult {
        if (shouldFail) {
            return TodoSyncResult.Failure(500, "Failure")
        }

        val currentIds = todos.map { it.id }.toSet()
        val orderedSet = orderedIds.toSet()
        if (currentIds != orderedSet) {
            return TodoSyncResult.Failure(400, "Bad order")
        }

        val reordered = orderedIds.mapNotNull { id -> todos.firstOrNull { it.id == id } }
        todos.clear()
        todos.addAll(reordered)
        return respondWithTodos()
    }

    private fun respondWithTodos(): TodoSyncResult =
        TodoSyncResult.Success(todos.toList(), 200)
}
