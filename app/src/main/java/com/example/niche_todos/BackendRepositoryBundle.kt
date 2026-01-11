// ABOUTME: Bundles backend repositories for injection into ViewModels.
// ABOUTME: Keeps health and auth repositories grouped for factory usage.
package com.example.niche_todos

data class BackendRepositoryBundle(
    val healthRepository: HealthRepository,
    val authRepository: AuthRepository,
    val todoRepository: TodoRepository
)
