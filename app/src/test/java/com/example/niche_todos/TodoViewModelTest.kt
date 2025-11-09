// ABOUTME: Unit tests for TodoViewModel
// Verifies add, update, delete, and toggle operations on todo list
package com.example.niche_todos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class TodoViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun addTodo_addsToList() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Buy milk")

            val todos = viewModel.todos.value
            assertNotNull(todos)
            assertEquals(1, todos?.size)
            assertEquals("Buy milk", todos?.get(0)?.text)
            assertEquals(false, todos?.get(0)?.isCompleted)
            assertNotNull(todos?.get(0)?.id)
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

            viewModel.addTodo("")
            viewModel.addTodo("   ")

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

            viewModel.addTodo("First")
            viewModel.addTodo("Second")
            viewModel.addTodo("Third")

            val todos = viewModel.todos.value
            assertEquals(3, todos?.size)
            assertEquals("First", todos?.get(0)?.text)
            assertEquals("Second", todos?.get(1)?.text)
            assertEquals("Third", todos?.get(2)?.text)
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

            viewModel.addTodo("Test task")
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

            viewModel.addTodo("Test task")
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

            viewModel.addTodo("First")
            viewModel.addTodo("Second")
            viewModel.addTodo("Third")

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

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original text")
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "Updated text")

            val updatedTodo = viewModel.todos.value?.get(0)
            assertEquals("Updated text", updatedTodo?.text)
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

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Original")
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""

            viewModel.updateTodo(todoId, "")
            assertEquals("Original", viewModel.todos.value?.get(0)?.text)

            viewModel.updateTodo(todoId, "   ")
            assertEquals("Original", viewModel.todos.value?.get(0)?.text)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_invalidId_noChange() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Test")
            val originalTodo = viewModel.todos.value?.get(0)

            viewModel.updateTodo("invalid-id", "Changed")

            assertEquals(originalTodo, viewModel.todos.value?.get(0))
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }

    @Test
    fun updateTodo_preservesCompletionStatus() {
        val viewModel = TodoViewModel()
        val observer = Observer<List<Todo>> {}

        try {
            viewModel.todos.observeForever(observer)

            viewModel.addTodo("Task")
            val todoId = viewModel.todos.value?.get(0)?.id ?: ""
            viewModel.toggleComplete(todoId)

            viewModel.updateTodo(todoId, "Updated task")

            val todo = viewModel.todos.value?.get(0)
            assertEquals("Updated task", todo?.text)
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

            viewModel.addTodo("Test task")
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

            viewModel.addTodo("Test")
            viewModel.addTodo("Test 2")
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

            viewModel.addTodo("First")
            viewModel.addTodo("Second")
            viewModel.addTodo("Third")

            val secondId = viewModel.todos.value?.get(1)?.id ?: ""
            viewModel.deleteTodo(secondId)

            val todos = viewModel.todos.value
            assertEquals(2, todos?.size)
            assertEquals("First", todos?.get(0)?.text)
            assertEquals("Third", todos?.get(1)?.text)
        } finally {
            viewModel.todos.removeObserver(observer)
        }
    }
}
