// ABOUTME: Unit tests for TodoViewModel
// Verifies add, update, delete, and toggle operations on todo list
package com.example.niche_todos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
        repository: TodoRepository = FakeTodoRepository()
    ): TodoViewModel = TodoViewModel(
        todoRepository = repository,
        nowProvider = nowProvider
    )

    private suspend fun TestScope.settleTodos() = advanceUntilIdle()

    @Test
    fun addTodo_addsToList() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 3, 1, 8, 0)
        val endDateTime = LocalDateTime.of(2025, 3, 1, 9, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Buy milk", startDateTime, endDateTime)

            settleTodos()

            val todos = viewModel.todos.value
            assertNotNull(todos)
            assertEquals(1, todos?.size)
            val todo = todos?.get(0)
            assertEquals("Buy milk", todo?.title)
            assertEquals(startDateTime, todo?.startDateTime)
            assertEquals(endDateTime, todo?.endDateTime)
            assertEquals(false, todo?.isCompleted)
            assertNotNull(todo?.id)
            assertEquals(1, todo?.properties?.filterIsInstance<TodoProperty.Title>()?.size)
            assertEquals(1, todo?.properties?.filterIsInstance<TodoProperty.StartDateTime>()?.size)
            assertEquals(1, todo?.properties?.filterIsInstance<TodoProperty.EndDateTime>()?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun addTodo_emptyText_notAdded() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("", null, null)
            viewModel.addTodo("   ", null, null)

            settleTodos()

            val todos = viewModel.todos.value
            assertNotNull(todos)
            assertEquals(0, todos?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun addTodo_multipleItems_preservesOrder() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

            settleTodos()

            val todos = viewModel.todos.value
            assertEquals(3, todos?.size)
            assertEquals("First", todos?.get(0)?.title)
            assertEquals("Second", todos?.get(1)?.title)
            assertEquals("Third", todos?.get(2)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun addTodo_missingDates_defaultsToCurrentDayRange() = runTest {
        val fixedNow = LocalDateTime.of(2025, 5, 1, 11, 30)
        val viewModel = buildViewModel(nowProvider = { fixedNow })
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Defaulted", null, null)

            settleTodos()

            val todo = viewModel.todos.value?.first()
            val expectedStart = LocalDateTime.of(2025, 5, 1, 0, 0)
            val expectedEnd = LocalDateTime.of(2025, 5, 1, 23, 59, 59)
            assertEquals(expectedStart, todo?.startDateTime)
            assertEquals(expectedEnd, todo?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun defaultDateRange_returnsCurrentDayBounds() = runTest {
        val fixedNow = LocalDateTime.of(2026, 1, 5, 16, 45)
        val viewModel = buildViewModel(nowProvider = { fixedNow })

        val (start, end) = viewModel.defaultDateRange()

        val expectedStart = LocalDateTime.of(2026, 1, 5, 0, 0)
        val expectedEnd = LocalDateTime.of(2026, 1, 5, 23, 59, 59)
        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun addTodo_startProvidedEndMissing_setsEndToEndOfDay() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 6, 2, 9, 15)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Task", startDateTime, null)

            settleTodos()

            val todo = viewModel.todos.value?.first()
            val expectedEnd = LocalDateTime.of(2025, 6, 2, 23, 59, 59)
            assertEquals(startDateTime, todo?.startDateTime)
            assertEquals(expectedEnd, todo?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun addTodo_endBeforeStart_clampsEndToStart() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 5, 2, 12, 0)
        val endDateTime = LocalDateTime.of(2025, 5, 2, 10, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Invalid range", startDateTime, endDateTime)

            settleTodos()

            val todo = viewModel.todos.value?.first()
            assertEquals(startDateTime, todo?.startDateTime)
            assertEquals(startDateTime, todo?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun toggleComplete_togglesCompletionStatus() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            settleTodos()

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.toggleComplete(todoId)
            settleTodos()

            assertEquals(true, viewModel.todos.value?.get(0)?.isCompleted)

            viewModel.toggleComplete(todoId)
            settleTodos()

            assertEquals(false, viewModel.todos.value?.get(0)?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun toggleComplete_invalidId_noChange() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            settleTodos()

            val originalTodo = viewModel.todos.value?.get(0)

            viewModel.toggleComplete("invalid-id-999")

            settleTodos()

            val todoAfter = viewModel.todos.value?.get(0)
            assertEquals(originalTodo, todoAfter)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun toggleComplete_multipleItems_onlyTogglesTarget() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

            settleTodos()

            val secondId = viewModel.todos.value?.get(1)?.id ?: ""
            viewModel.toggleComplete(secondId)

            settleTodos()

            val todos = viewModel.todos.value
            assertEquals(false, todos?.get(0)?.isCompleted)
            assertEquals(true, todos?.get(1)?.isCompleted)
            assertEquals(false, todos?.get(2)?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_updatesText() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 10, 13, 15)
        val endDateTime = LocalDateTime.of(2025, 4, 10, 14, 45)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original text", null, null)
            settleTodos()

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "Updated text", startDateTime, endDateTime)

            settleTodos()

            val updatedTodo = viewModel.todos.value?.get(0)
            assertEquals("Updated text", updatedTodo?.title)
            assertEquals(startDateTime, updatedTodo?.startDateTime)
            assertEquals(endDateTime, updatedTodo?.endDateTime)
            assertEquals(todoId, updatedTodo?.id)
            assertEquals(false, updatedTodo?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_endBeforeStart_clampsEndToStart() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val originalStart = LocalDateTime.of(2025, 5, 3, 9, 0)
        val originalEnd = LocalDateTime.of(2025, 5, 3, 17, 0)
        val updatedStart = LocalDateTime.of(2025, 5, 4, 8, 0)
        val invalidEnd = LocalDateTime.of(2025, 5, 4, 7, 30)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Task", originalStart, originalEnd)
            settleTodos()

            val todoId = viewModel.todos.value?.first()?.id ?: ""

            viewModel.updateTodo(todoId, "Task", updatedStart, invalidEnd)

            settleTodos()

            val todo = viewModel.todos.value?.first()
            assertEquals(updatedStart, todo?.startDateTime)
            assertEquals(updatedStart, todo?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_emptyText_notUpdated() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val originalStartDateTime = LocalDateTime.of(2025, 4, 11, 9, 0)
        val originalEndDateTime = LocalDateTime.of(2025, 4, 11, 10, 0)
        val updatedStartDateTime = LocalDateTime.of(2025, 4, 12, 11, 0)
        val updatedEndDateTime = LocalDateTime.of(2025, 4, 12, 12, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original", originalStartDateTime, originalEndDateTime)
            settleTodos()

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "", updatedStartDateTime, updatedEndDateTime)
            settleTodos()

            assertEquals("Original", viewModel.todos.value?.get(0)?.title)
            settleTodos()

            assertEquals(originalStartDateTime, viewModel.todos.value?.get(0)?.startDateTime)
            settleTodos()

            assertEquals(originalEndDateTime, viewModel.todos.value?.get(0)?.endDateTime)

            viewModel.updateTodo(todoId, "   ", updatedStartDateTime, updatedEndDateTime)
            settleTodos()

            assertEquals("Original", viewModel.todos.value?.get(0)?.title)
            settleTodos()

            assertEquals(originalStartDateTime, viewModel.todos.value?.get(0)?.startDateTime)
            settleTodos()

            assertEquals(originalEndDateTime, viewModel.todos.value?.get(0)?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_invalidId_noChange() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 13, 15, 0)
        val endDateTime = LocalDateTime.of(2025, 4, 13, 16, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test", null, null)
            settleTodos()

            val originalTodo = viewModel.todos.value?.get(0)

            viewModel.updateTodo("invalid-id", "Changed", startDateTime, endDateTime)

            settleTodos()

            assertEquals(originalTodo, viewModel.todos.value?.get(0))
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_preservesCompletionStatus() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 14, 8, 30)
        val endDateTime = LocalDateTime.of(2025, 4, 14, 9, 30)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Task", null, null)
            settleTodos()

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""
            viewModel.toggleComplete(todoId)

            viewModel.updateTodo(todoId, "Updated task", startDateTime, endDateTime)

            settleTodos()

            val todo = viewModel.todos.value?.get(0)
            assertEquals("Updated task", todo?.title)
            assertEquals(startDateTime, todo?.startDateTime)
            assertEquals(endDateTime, todo?.endDateTime)
            assertEquals(true, todo?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun deleteTodo_removesFromList() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            settleTodos()

            assertEquals(1, viewModel.todos.value?.size)

            settleTodos()

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""
            viewModel.deleteTodo(todoId)

            settleTodos()

            assertEquals(0, viewModel.todos.value?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun deleteTodo_invalidId_noChange() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test", null, null)
            viewModel.addTodo("Test 2", null, null)
            settleTodos()

            assertEquals(2, viewModel.todos.value?.size)

            viewModel.deleteTodo("invalid-id")

            settleTodos()

            assertEquals(2, viewModel.todos.value?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun deleteTodo_multipleItems_onlyRemovesTarget() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

            settleTodos()

            val secondId = viewModel.todos.value?.get(1)?.id ?: ""
            viewModel.deleteTodo(secondId)

            settleTodos()

            val todos = viewModel.todos.value
            assertEquals(2, todos?.size)
            assertEquals("First", todos?.get(0)?.title)
            assertEquals("Third", todos?.get(1)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun moveTodo_validIndices_reordersList() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("One", null, null)
            viewModel.addTodo("Two", null, null)
            viewModel.addTodo("Three", null, null)

            viewModel.moveTodo(0, 2)

            settleTodos()

            val todos = viewModel.todos.value
            assertEquals("Two", todos?.get(0)?.title)
            assertEquals("Three", todos?.get(1)?.title)
            assertEquals("One", todos?.get(2)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun moveTodo_invalidIndex_noChange() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Alpha", null, null)
            viewModel.addTodo("Beta", null, null)

            settleTodos()

            val original = viewModel.todos.value

            viewModel.moveTodo(-1, 1)
            settleTodos()

            assertEquals(original, viewModel.todos.value)

            viewModel.moveTodo(0, 5)
            settleTodos()

            assertEquals(original, viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun reorderTodos_matchingIds_updatesOrder() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("One", null, null)
            viewModel.addTodo("Two", null, null)
            viewModel.addTodo("Three", null, null)

            settleTodos()

            val reversed = viewModel.todos.value?.reversed() ?: emptyList()
            viewModel.reorderTodos(reversed)

            settleTodos()

            val todos = viewModel.todos.value
            assertEquals("Three", todos?.get(0)?.title)
            assertEquals("Two", todos?.get(1)?.title)
            assertEquals("One", todos?.get(2)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun reorderTodos_mismatchedIds_noChange() = runTest {
        val viewModel = buildViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)

            val fakeList = listOf(
                Todo("custom-1", emptyList(), false),
                Todo("custom-2", emptyList(), false)
            )
            settleTodos()

            val original = viewModel.todos.value

            viewModel.reorderTodos(fakeList)

            settleTodos()

            assertEquals(original, viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun refreshTodos_updatesListFromRepository() = runTest {
        val todo = Todo(
            id = "server-1",
            properties = listOf(
                TodoProperty.Title("Synced"),
                TodoProperty.StartDateTime(null),
                TodoProperty.EndDateTime(null)
            ),
            isCompleted = false
        )
        val repository = FakeTodoRepository(initialTodos = listOf(todo))
        val viewModel = buildViewModel(repository = repository)
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.refreshTodos()

            settleTodos()

            assertEquals(listOf(todo), viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun refreshTodos_failureLeavesListUnchanged() = runTest {
        val repository = FakeTodoRepository(shouldFail = true)
        val viewModel = buildViewModel(repository = repository)
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.refreshTodos()

            settleTodos()

            assertEquals(emptyList<Todo>(), viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }
}
