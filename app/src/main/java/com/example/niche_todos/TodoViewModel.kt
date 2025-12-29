// ABOUTME: ViewModel for managing todo list state and operations
// Handles add, update, delete, and toggle completion for todos
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDateTime
import java.util.UUID

class TodoViewModel(
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() }
) : ViewModel() {

    private val _todos = MutableLiveData<List<Todo>>(emptyList())
    val todos: LiveData<List<Todo>> = _todos

    private fun currentDayBounds(): Pair<LocalDateTime, LocalDateTime> {
        val today = nowProvider().toLocalDate()
        val startOfDay = today.atStartOfDay()
        val endOfDay = TodoDateDefaults.endOfDay(today)
        return startOfDay to endOfDay
    }

    fun defaultDateRange(): Pair<LocalDateTime, LocalDateTime> = currentDayBounds()

    private fun prepareAddDates(
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): Pair<LocalDateTime, LocalDateTime> {
        val (defaultStart, defaultEnd) = currentDayBounds()
        val resolvedStart = startDateTime ?: defaultStart
        val resolvedEnd = when (endDateTime) {
            null -> if (startDateTime != null) {
                LocalDateTime.of(startDateTime.toLocalDate(), TodoDateDefaults.END_OF_DAY_TIME)
            } else {
                defaultEnd
            }
            else -> endDateTime
        }
        val adjustedEnd = if (resolvedEnd.isBefore(resolvedStart)) {
            resolvedStart
        } else {
            resolvedEnd
        }
        return resolvedStart to adjustedEnd
    }

    private fun enforceEndAfterStart(
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?
    ): Pair<LocalDateTime?, LocalDateTime?> {
        if (startDateTime != null && endDateTime != null && endDateTime.isBefore(startDateTime)) {
            return startDateTime to startDateTime
        }
        return startDateTime to endDateTime
    }

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

        val (resolvedStart, resolvedEnd) = prepareAddDates(startDateTime, endDateTime)

        val newTodo = Todo(
            id = UUID.randomUUID().toString(),
            properties = buildProperties(trimmedText, resolvedStart, resolvedEnd),
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

        val (resolvedStart, resolvedEnd) = enforceEndAfterStart(startDateTime, endDateTime)

        val currentList = _todos.value ?: return
        _todos.value = currentList.map { todo ->
            if (todo.id == id) {
                todo.copy(
                    properties = updateProperties(
                        todo.properties,
                        trimmedText,
                        resolvedStart,
                        resolvedEnd
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
