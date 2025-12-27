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

    private fun buildProperties(title: String): List<TodoProperty> {
        return listOf(
            TodoProperty.Title(title),
            TodoProperty.StartDateTime(null),
            TodoProperty.EndDateTime(null)
        )
    }

    private fun updateTitle(properties: List<TodoProperty>, title: String): List<TodoProperty> {
        var hasTitle = false
        val updated = properties.map { property ->
            if (property is TodoProperty.Title) {
                hasTitle = true
                TodoProperty.Title(title)
            } else {
                property
            }
        }
        return if (hasTitle) {
            updated
        } else {
            listOf(TodoProperty.Title(title)) + updated
        }
    }

    fun addTodo(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val newTodo = Todo(
            id = UUID.randomUUID().toString(),
            properties = buildProperties(trimmedText),
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
                todo.copy(properties = updateTitle(todo.properties, trimmedText))
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
