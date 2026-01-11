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

interface HealthRepository {
    suspend fun runHealthCheck(): HealthCheckResult
}

interface AuthRepository {
    suspend fun exchangeGoogleIdToken(idToken: String): AuthResult
}

class BackendHealthRepository(
    private val client: HealthCheckClient,
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
        if (response.statusCode == 200 && tokens != null) {
            tokenStore.save(tokens)
            return AuthResult.Success(tokens, response.statusCode)
        }

        val message = response.problemDetails?.detail ?: response.errorBody
        return AuthResult.Failure(response.statusCode, message)
    }
}
