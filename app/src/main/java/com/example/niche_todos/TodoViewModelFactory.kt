// ABOUTME: Factory for constructing TodoViewModel with backend repositories.
// ABOUTME: Keeps ViewModel creation consistent and injectable for tests.
package com.example.niche_todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDateTime

class TodoViewModelFactory(
    private val todoRepository: TodoRepository,
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() }
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(
                todoRepository = todoRepository,
                nowProvider = nowProvider
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
