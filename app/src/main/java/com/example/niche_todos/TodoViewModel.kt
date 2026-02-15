// ABOUTME: ViewModel for managing todo list state and operations
// Handles add, update, delete, and toggle completion for todos
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
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
    private val _syncError = MutableLiveData<String?>(null)
    val syncError: LiveData<String?> = _syncError
    private val _collapsedTodoIds = MutableLiveData<Set<String>>(emptySet())
    val collapsedTodoIds: LiveData<Set<String>> = _collapsedTodoIds
    val visibleTodos: LiveData<List<Todo>> = MediatorLiveData<List<Todo>>().apply {
        addSource(_todos) { todos ->
            val collapsedIds = _collapsedTodoIds.value ?: emptySet()
            value = TodoHierarchyUtils.visibleTodos(todos, collapsedIds)
        }
        addSource(_collapsedTodoIds) { collapsedIds ->
            val todos = _todos.value ?: emptyList()
            value = TodoHierarchyUtils.visibleTodos(todos, collapsedIds)
        }
    }
    val hasChildrenIds: LiveData<Set<String>> = MediatorLiveData<Set<String>>().apply {
        addSource(_todos) { value = TodoHierarchyUtils.hasChildrenIds(it) }
    }

    private fun currentDayBounds(): Pair<LocalDateTime, LocalDateTime> {
        val today = nowProvider().toLocalDate()
        val startOfDay = today.atStartOfDay()
        val endOfDay = TodoDateDefaults.endOfDay(today)
        return startOfDay to endOfDay
    }

    // Debug-only escape hatch for running UI/voice flows without backend auth.
    // This is intentionally not exposed as LiveData to avoid accidental production use.
    private var debugLocalOnlyMode: Boolean = false

    internal fun debugEnableLocalOnlyMode(enabled: Boolean) {
        debugLocalOnlyMode = enabled
    }

    internal fun debugReplaceTodos(newTodos: List<Todo>) {
        updateTodos(newTodos)
    }

    fun defaultDateRange(): Pair<LocalDateTime, LocalDateTime> = currentDayBounds()

    fun refreshTodos() {
        viewModelScope.launch {
            when (val result = todoRepository.fetchTodos()) {
                is TodoSyncResult.Success -> {
                    _syncError.value = null
                    updateTodos(result.todos)
                }
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Fetch failed: ${syncFailureMessage(result)}"
                }
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
                is TodoSyncResult.Success -> {
                    _syncError.value = null
                    updateTodos(result.todos)
                }
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Create failed: ${syncFailureMessage(result)}"
                }
            }
        }
    }

    fun toggleComplete(id: String) {
        val currentList = _todos.value ?: return
        val todo = currentList.firstOrNull { it.id == id } ?: return
        val updatedCompleted = !todo.isCompleted
        // Optimistically update UI; if backend rejects, revert and surface error.
        val optimistic = todo.copy(isCompleted = updatedCompleted)
        _todos.value = currentList.map { item -> if (item.id == id) optimistic else item }
        _syncError.value = null

        if (debugLocalOnlyMode) {
            return
        }

        viewModelScope.launch {
            when (val result = todoRepository.updateTodo(
                id = todo.id,
                title = todo.title,
                startDateTime = todo.startDateTime,
                endDateTime = todo.endDateTime,
                isCompleted = updatedCompleted
            )) {
                is TodoSyncResult.Success -> updateTodos(result.todos)
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Update failed: ${syncFailureMessage(result)}"
                    val latest = _todos.value ?: emptyList()
                    _todos.value = latest.map { item -> if (item.id == id) todo else item }
                }
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
                is TodoSyncResult.Success -> {
                    _syncError.value = null
                    updateTodos(result.todos)
                }
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Update failed: ${syncFailureMessage(result)}"
                }
            }
        }
    }

    fun deleteTodo(id: String) {
        _todos.value ?: return
        viewModelScope.launch {
            when (val result = todoRepository.deleteTodo(id)) {
                is TodoSyncResult.Success -> {
                    _syncError.value = null
                    updateTodos(result.todos)
                }
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Delete failed: ${syncFailureMessage(result)}"
                }
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
        updateTodos(mutableList.toList())
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
                is TodoSyncResult.Success -> {
                    _syncError.value = null
                    updateTodos(result.todos)
                }
                is TodoSyncResult.Failure -> {
                    _syncError.value = "Reorder failed: ${syncFailureMessage(result)}"
                }
            }
        }
    }

    fun toggleCollapsed(todoId: String) {
        val currentCollapsed = _collapsedTodoIds.value ?: emptySet()
        val newCollapsed = currentCollapsed.toMutableSet()
        if (!newCollapsed.add(todoId)) {
            newCollapsed.remove(todoId)
        }
        _collapsedTodoIds.value = newCollapsed
    }

    private fun updateTodos(newTodos: List<Todo>) {
        _todos.value = newTodos
        val existingIds = newTodos.map { it.id }.toSet()
        val currentCollapsed = _collapsedTodoIds.value ?: emptySet()
        if (currentCollapsed.isNotEmpty()) {
            val prunedCollapsed = currentCollapsed.intersect(existingIds)
            if (prunedCollapsed.size != currentCollapsed.size) {
                _collapsedTodoIds.value = prunedCollapsed
            }
        }
    }

    private fun syncFailureMessage(failure: TodoSyncResult.Failure): String {
        val code = failure.statusCode?.let { "HTTP $it" } ?: "no status"
        val details = failure.message?.takeIf { it.isNotBlank() }
        return if (details == null) code else "$code ($details)"
    }
}
