// ABOUTME: ViewModel for managing todo list state and operations
// Handles add, update, delete, and toggle completion for todos
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime
import java.util.UUID

class TodoViewModel : ViewModel() {
    private val _todos = MutableLiveData<List<Todo>>(emptyList())
    val todos: LiveData<List<Todo>> = _todos

    private fun buildProperties(
        title: String,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): List<TodoProperty> {
        return listOf(
            TodoProperty.Title(title),
            TodoProperty.StartDateTime(startDateTime),
            TodoProperty.EndDateTime(endDateTime)
        )
    }

    private fun updateProperties(
        properties: List<TodoProperty>,
        title: String,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): List<TodoProperty> {
        val remaining = properties.filterNot { property ->
            property is TodoProperty.Title ||
                property is TodoProperty.StartDateTime ||
                property is TodoProperty.EndDateTime
        }
        return listOf(
            TodoProperty.Title(title),
            TodoProperty.StartDateTime(startDateTime),
            TodoProperty.EndDateTime(endDateTime)
        ) + remaining
    }

    fun addTodo(text: String, startDateTime: LocalDateTime?, endDateTime: LocalDateTime?) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val newTodo = Todo(
            id = UUID.randomUUID().toString(),
            properties = buildProperties(trimmedText, startDateTime, endDateTime),
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

    fun updateTodo(
        id: String,
        newText: String,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ) {
        val trimmedText = newText.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val currentList = _todos.value ?: return
        _todos.value = currentList.map { todo ->
            if (todo.id == id) {
                todo.copy(
                    properties = updateProperties(
                        todo.properties,
                        trimmedText,
                        startDateTime,
                        endDateTime
                    )
                )
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
