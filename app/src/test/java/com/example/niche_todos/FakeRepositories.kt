// ABOUTME: Test doubles for backend repositories.
// ABOUTME: Lets tests control backend responses deterministically.
package com.example.niche_todos

class FakeHealthRepository(
    private val result: HealthCheckResult
) : HealthRepository {
    override suspend fun runHealthCheck(): HealthCheckResult = result
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
