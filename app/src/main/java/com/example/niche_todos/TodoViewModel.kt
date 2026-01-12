// ABOUTME: ViewModel for managing todo list state and operations
// Handles add, update, delete, and toggle completion for todos
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import kotlinx.coroutines.launch

class TodoViewModel(
    private val todoRepository: TodoRepository,
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

    fun refreshTodos() {
        viewModelScope.launch {
            when (val result = todoRepository.fetchTodos()) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
            }
        }
    }

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

    fun addTodo(
        text: String,
        startDateTime: LocalDateTime?,
        endDateTime: LocalDateTime?,
        parentId: String? = null
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return
        }

        val (resolvedStart, resolvedEnd) = prepareAddDates(startDateTime, endDateTime)

        viewModelScope.launch {
            when (val result = todoRepository.createTodo(
                trimmedText,
                resolvedStart,
                resolvedEnd,
                false,
                parentId
            )) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
            }
        }
    }

    fun toggleComplete(id: String) {
        val currentList = _todos.value ?: return
        val todo = currentList.firstOrNull { it.id == id } ?: return
        val updatedCompleted = !todo.isCompleted
        viewModelScope.launch {
            when (val result = todoRepository.updateTodo(
                id = todo.id,
                title = todo.title,
                startDateTime = todo.startDateTime,
                endDateTime = todo.endDateTime,
                isCompleted = updatedCompleted
            )) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
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
        val todo = currentList.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            when (val result = todoRepository.updateTodo(
                id = todo.id,
                title = trimmedText,
                startDateTime = resolvedStart,
                endDateTime = resolvedEnd,
                isCompleted = todo.isCompleted
            )) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
            }
        }
    }

    fun deleteTodo(id: String) {
        _todos.value ?: return
        viewModelScope.launch {
            when (val result = todoRepository.deleteTodo(id)) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
            }
        }
    }

    fun moveTodo(fromIndex: Int, toIndex: Int) {
        val currentList = _todos.value ?: return
        if (fromIndex == toIndex ||
            fromIndex !in currentList.indices ||
            toIndex !in currentList.indices
        ) {
            return
        }

        val mutableList = currentList.toMutableList()
        val todo = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, todo)
        _todos.value = mutableList.toList()
    }

    fun reorderTodos(items: List<ReorderTodoItem>) {
        val currentList = _todos.value ?: return
        if (currentList.size != items.size) {
            return
        }
        val currentIds = currentList.map { it.id }.toSet()
        val newIds = items.map { it.id }.toSet()
        if (currentIds != newIds) {
            return
        }
        viewModelScope.launch {
            when (val result = todoRepository.reorderTodos(items)) {
                is TodoSyncResult.Success -> _todos.value = result.todos
                is TodoSyncResult.Failure -> Unit
            }
        }
    }
}
