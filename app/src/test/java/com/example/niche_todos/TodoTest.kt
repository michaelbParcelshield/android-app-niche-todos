// ABOUTME: Unit tests for Todo data class
// Verifies Todo creation, properties, and data class equality
package com.example.niche_todos

import org.junit.Test
import org.junit.Assert.*

class TodoTest {
    @Test
    fun todo_hasRequiredProperties() {
        val todo = Todo(
            id = "test-id-123",
            text = "Buy milk",
            isCompleted = false
        )

        assertEquals("test-id-123", todo.id)
        assertEquals("Buy milk", todo.text)
        assertEquals(false, todo.isCompleted)
    }

    @Test
    fun todo_dataClassEquality() {
        val todo1 = Todo("id1", "Task 1", false)
        val todo2 = Todo("id1", "Task 1", false)
        val todo3 = Todo("id2", "Task 2", true)

        assertEquals(todo1, todo2)
        assertNotEquals(todo1, todo3)
    }

    @Test
    fun todo_copyWithChanges() {
        val original = Todo("id1", "Original", false)
        val completed = original.copy(isCompleted = true)
        val updated = original.copy(text = "Updated")

        assertEquals("id1", completed.id)
        assertEquals("Original", completed.text)
        assertEquals(true, completed.isCompleted)

        assertEquals("id1", updated.id)
        assertEquals("Updated", updated.text)
        assertEquals(false, updated.isCompleted)
    }
}
