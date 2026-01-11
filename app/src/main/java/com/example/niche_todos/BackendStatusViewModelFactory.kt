// ABOUTME: Factory for constructing BackendStatusViewModel with repositories.
// ABOUTME: Keeps ViewModel creation consistent and injectable for tests.
package com.example.niche_todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BackendStatusViewModelFactory(
    private val repositories: BackendRepositories
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackendStatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackendStatusViewModel(
                healthRepository = repositories.healthRepository,
                authRepository = repositories.authRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
