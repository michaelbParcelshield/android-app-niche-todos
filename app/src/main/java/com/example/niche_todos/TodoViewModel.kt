// ABOUTME: ViewModel for managing todo list state and operations
// Handles add, update, delete, and toggle completion for todos
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.UUID

class TodoViewModel : ViewModel() {
    private val _todos = MutableLiveData<List<Todo>>(emptyList())
    val todos: LiveData<List<Todo>> = _todos

    fun addTodo(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val newTodo = Todo(
            id = UUID.randomUUID().toString(),
            text = trimmedText,
            isCompleted = false
        )

        val currentList = _todos.value ?: emptyList()
        _todos.value = currentList + newTodo
    }

    fun toggleComplete(id: String) {
        val currentList = _todos.value ?: return
        _todos.value = currentList.map { todo ->
            if (todo.id == id) {
                todo.copy(isCompleted = !todo.isCompleted)
            } else {
                todo
            }
        }
    }

    fun updateTodo(id: String, newText: String) {
        val trimmedText = newText.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val currentList = _todos.value ?: return
        _todos.value = currentList.map { todo ->
            if (todo.id == id) {
                todo.copy(text = trimmedText)
            } else {
                todo
            }
        }
    }

    fun deleteTodo(id: String) {
        val currentList = _todos.value ?: return
        _todos.value = currentList.filter { todo -> todo.id != id }
    }
}
