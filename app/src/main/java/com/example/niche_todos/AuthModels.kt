// ABOUTME: Auth data models for backend requests and responses.
// ABOUTME: Used for JSON serialization and in-memory token handling.
package com.example.niche_todos

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class AuthenticatedUser(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val expiresInSeconds: Int,
    val refreshToken: String,
    val user: AuthenticatedUser
)

@Serializable
data class ProblemDetails(
    val title: String? = null,
    val detail: String? = null
)
