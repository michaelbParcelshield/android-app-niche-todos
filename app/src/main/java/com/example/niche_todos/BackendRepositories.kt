// ABOUTME: Repository abstractions for backend network operations.
// ABOUTME: Encapsulates health check and auth exchanges for ViewModels.
package com.example.niche_todos

sealed class HealthCheckResult {
    data class Success(val statusCode: Int) : HealthCheckResult()
    data class Failure(val statusCode: Int?, val message: String?) : HealthCheckResult()
}

sealed class AuthResult {
    data class Success(val tokens: AuthTokens, val statusCode: Int) : AuthResult()
    data class Failure(val statusCode: Int?, val message: String?) : AuthResult()
}

sealed class TodoSyncResult {
    data class Success(val todos: List<Todo>, val statusCode: Int) : TodoSyncResult()
    data class Failure(val statusCode: Int?, val message: String?) : TodoSyncResult()
}

interface HealthRepository {
    suspend fun runHealthCheck(): HealthCheckResult
}

interface AuthRepository {
    suspend fun exchangeGoogleIdToken(idToken: String): AuthResult
}

interface TodoRepository {
    suspend fun fetchTodos(): TodoSyncResult
    suspend fun createTodo(
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult
    suspend fun updateTodo(
        id: String,
        title: String,
        startDateTime: java.time.LocalDateTime?,
        endDateTime: java.time.LocalDateTime?,
        isCompleted: Boolean
    ): TodoSyncResult
    suspend fun deleteTodo(id: String): TodoSyncResult
    suspend fun reorderTodos(orderedIds: List<String>): TodoSyncResult
}

class BackendHealthRepository(
    private val client: HealthClient,
    private val endpoints: BackendEndpoints
) : HealthRepository {
    override suspend fun runHealthCheck(): HealthCheckResult {
        val statusCode = client.fetchStatusCode(endpoints.healthUrl)
        return if (statusCode == 200) {
            HealthCheckResult.Success(statusCode)
        } else {
            val statusLabel = statusCode?.toString() ?: "unknown"
            HealthCheckResult.Failure(statusCode, "Unexpected status: $statusLabel")
        }
    }
}

class BackendAuthRepository(
    private val client: AuthClient,
    private val endpoints: BackendEndpoints,
    private val tokenStore: AuthTokenStore
) : AuthRepository {
    override suspend fun exchangeGoogleIdToken(idToken: String): AuthResult {
        val response = client.exchangeGoogleIdToken(endpoints.authUrl, idToken)
            ?: return AuthResult.Failure(null, "Network error")

        val tokens = response.tokens
        if (response.statusCode in 200..299 && tokens != null) {
            tokenStore.save(tokens)
            return AuthResult.Success(tokens, response.statusCode)
        }

        val message = response.problemDetails?.detail ?: "Authentication failed"
        return AuthResult.Failure(response.statusCode, message)
    }
}
