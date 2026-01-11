// ABOUTME: Unit tests for BackendAuthRepository.
// ABOUTME: Validates token persistence and error detail propagation.
package com.example.niche_todos

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.net.URL

class BackendAuthRepositoryTest {

    @Test
    fun exchangeGoogleIdToken_savesTokensOnSuccess() = runTest {
        val tokens = AuthTokens(
            accessToken = "access",
            expiresInSeconds = 3600,
            refreshToken = "refresh",
            user = AuthenticatedUser(
                id = "user-id",
                email = "user@example.com",
                name = "User",
                avatarUrl = null
            )
        )
        val tokenStore = FakeAuthTokenStore()
        val repository = BackendAuthRepository(
            client = FakeBackendAuthClient(
                BackendAuthResponse(
                    statusCode = 201,
                    tokens = tokens,
                    problemDetails = null,
                    errorBody = null
                )
            ),
            endpoints = BackendEndpoints(
                healthUrl = URL("https://example.com/healthz"),
                authUrl = URL("https://example.com/auth/google"),
                todosUrl = URL("https://example.com/todos")
            ),
            tokenStore = tokenStore
        )

        val result = repository.exchangeGoogleIdToken("token-123")

        assertEquals(AuthResult.Success(tokens, 201), result)
        assertSame(tokens, tokenStore.savedTokens)
    }

    @Test
    fun exchangeGoogleIdToken_prefersProblemDetailsMessage() = runTest {
        val tokenStore = FakeAuthTokenStore()
        val repository = BackendAuthRepository(
            client = FakeBackendAuthClient(
                BackendAuthResponse(
                    statusCode = 401,
                    tokens = null,
                    problemDetails = ProblemDetails(
                        title = "Unauthorized",
                        detail = "Token invalid"
                    ),
                    errorBody = "{\"detail\":\"Ignored\"}"
                )
            ),
            endpoints = BackendEndpoints(
                healthUrl = URL("https://example.com/healthz"),
                authUrl = URL("https://example.com/auth/google"),
                todosUrl = URL("https://example.com/todos")
            ),
            tokenStore = tokenStore
        )

        val result = repository.exchangeGoogleIdToken("token-123")

        assertEquals(AuthResult.Failure(401, "Token invalid"), result)
        assertNull(tokenStore.savedTokens)
    }

    @Test
    fun exchangeGoogleIdToken_usesFallbackWhenProblemDetailsMissing() = runTest {
        val tokenStore = FakeAuthTokenStore()
        val repository = BackendAuthRepository(
            client = FakeBackendAuthClient(
                BackendAuthResponse(
                    statusCode = 500,
                    tokens = null,
                    problemDetails = null,
                    errorBody = "Server exploded"
                )
            ),
            endpoints = BackendEndpoints(
                healthUrl = URL("https://example.com/healthz"),
                authUrl = URL("https://example.com/auth/google"),
                todosUrl = URL("https://example.com/todos")
            ),
            tokenStore = tokenStore
        )

        val result = repository.exchangeGoogleIdToken("token-123")

        assertEquals(AuthResult.Failure(500, null), result)
        assertNull(tokenStore.savedTokens)
    }
}
