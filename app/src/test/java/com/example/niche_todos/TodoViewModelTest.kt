// ABOUTME: Unit tests for TodoViewModel
// Verifies add, update, delete, and toggle operations on todo list
package com.example.niche_todos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class TodoViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun addTodo_addsToList() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 3, 1, 8, 0)
        val endDateTime = LocalDateTime.of(2025, 3, 1, 9, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Buy milk", startDateTime, endDateTime)

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
    fun addTodo_emptyText_notAdded() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("", null, null)
            viewModel.addTodo("   ", null, null)

            val todos = viewModel.todos.value
            assertNotNull(todos)
            assertEquals(0, todos?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun addTodo_multipleItems_preservesOrder() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

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
    fun toggleComplete_togglesCompletionStatus() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.toggleComplete(todoId)
            assertEquals(true, viewModel.todos.value?.get(0)?.isCompleted)

            viewModel.toggleComplete(todoId)
            assertEquals(false, viewModel.todos.value?.get(0)?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun toggleComplete_invalidId_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            val originalTodo = viewModel.todos.value?.get(0)

            viewModel.toggleComplete("invalid-id-999")

            val todoAfter = viewModel.todos.value?.get(0)
            assertEquals(originalTodo, todoAfter)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun toggleComplete_multipleItems_onlyTogglesTarget() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

            val secondId = viewModel.todos.value?.get(1)?.id ?: ""
            viewModel.toggleComplete(secondId)

            val todos = viewModel.todos.value
            assertEquals(false, todos?.get(0)?.isCompleted)
            assertEquals(true, todos?.get(1)?.isCompleted)
            assertEquals(false, todos?.get(2)?.isCompleted)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_updatesText() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 10, 13, 15)
        val endDateTime = LocalDateTime.of(2025, 4, 10, 14, 45)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original text", null, null)
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "Updated text", startDateTime, endDateTime)

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
    fun updateTodo_emptyText_notUpdated() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}
        val originalStartDateTime = LocalDateTime.of(2025, 4, 11, 9, 0)
        val originalEndDateTime = LocalDateTime.of(2025, 4, 11, 10, 0)
        val updatedStartDateTime = LocalDateTime.of(2025, 4, 12, 11, 0)
        val updatedEndDateTime = LocalDateTime.of(2025, 4, 12, 12, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original", originalStartDateTime, originalEndDateTime)
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "", updatedStartDateTime, updatedEndDateTime)
            assertEquals("Original", viewModel.todos.value?.get(0)?.title)
            assertEquals(originalStartDateTime, viewModel.todos.value?.get(0)?.startDateTime)
            assertEquals(originalEndDateTime, viewModel.todos.value?.get(0)?.endDateTime)

            viewModel.updateTodo(todoId, "   ", updatedStartDateTime, updatedEndDateTime)
            assertEquals("Original", viewModel.todos.value?.get(0)?.title)
            assertEquals(originalStartDateTime, viewModel.todos.value?.get(0)?.startDateTime)
            assertEquals(originalEndDateTime, viewModel.todos.value?.get(0)?.endDateTime)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_invalidId_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 13, 15, 0)
        val endDateTime = LocalDateTime.of(2025, 4, 13, 16, 0)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test", null, null)
            val originalTodo = viewModel.todos.value?.get(0)

            viewModel.updateTodo("invalid-id", "Changed", startDateTime, endDateTime)

            assertEquals(originalTodo, viewModel.todos.value?.get(0))
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_preservesCompletionStatus() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}
        val startDateTime = LocalDateTime.of(2025, 4, 14, 8, 30)
        val endDateTime = LocalDateTime.of(2025, 4, 14, 9, 30)

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Task", null, null)
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""
            viewModel.toggleComplete(todoId)

            viewModel.updateTodo(todoId, "Updated task", startDateTime, endDateTime)

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
    fun deleteTodo_removesFromList() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test task", null, null)
            assertEquals(1, viewModel.todos.value?.size)

            val todoId = viewModel.todos.value?.get(0)?.id ?: ""
            viewModel.deleteTodo(todoId)

            assertEquals(0, viewModel.todos.value?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun deleteTodo_invalidId_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test", null, null)
            viewModel.addTodo("Test 2", null, null)
            assertEquals(2, viewModel.todos.value?.size)

            viewModel.deleteTodo("invalid-id")

            assertEquals(2, viewModel.todos.value?.size)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun deleteTodo_multipleItems_onlyRemovesTarget() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)
            viewModel.addTodo("Third", null, null)

            val secondId = viewModel.todos.value?.get(1)?.id ?: ""
            viewModel.deleteTodo(secondId)

            val todos = viewModel.todos.value
            assertEquals(2, todos?.size)
            assertEquals("First", todos?.get(0)?.title)
            assertEquals("Third", todos?.get(1)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun moveTodo_validIndices_reordersList() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("One", null, null)
            viewModel.addTodo("Two", null, null)
            viewModel.addTodo("Three", null, null)

            viewModel.moveTodo(0, 2)

            val todos = viewModel.todos.value
            assertEquals("Two", todos?.get(0)?.title)
            assertEquals("Three", todos?.get(1)?.title)
            assertEquals("One", todos?.get(2)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun moveTodo_invalidIndex_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Alpha", null, null)
            viewModel.addTodo("Beta", null, null)

            val original = viewModel.todos.value

            viewModel.moveTodo(-1, 1)
            assertEquals(original, viewModel.todos.value)

            viewModel.moveTodo(0, 5)
            assertEquals(original, viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun reorderTodos_matchingIds_updatesOrder() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("One", null, null)
            viewModel.addTodo("Two", null, null)
            viewModel.addTodo("Three", null, null)

            val reversed = viewModel.todos.value?.reversed() ?: emptyList()
            viewModel.reorderTodos(reversed)

            val todos = viewModel.todos.value
            assertEquals("Three", todos?.get(0)?.title)
            assertEquals("Two", todos?.get(1)?.title)
            assertEquals("One", todos?.get(2)?.title)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun reorderTodos_mismatchedIds_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("First", null, null)
            viewModel.addTodo("Second", null, null)

            val fakeList = listOf(
                Todo("custom-1", emptyList(), false),
                Todo("custom-2", emptyList(), false)
            )
            val original = viewModel.todos.value

            viewModel.reorderTodos(fakeList)

            assertEquals(original, viewModel.todos.value)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }
}
