// ABOUTME: Unit tests for Todo data class
// Verifies Todo creation, properties, and data class equality
package com.example.niche_todos

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class TodoTest {
    @Test
    fun todo_hasRequiredProperties() {
        val startDateTime = LocalDateTime.of(2025, 2, 12, 9, 30)
        val endDateTime = LocalDateTime.of(2025, 2, 12, 10, 45)
        val todo = Todo(
            id = "test-id-123",
            properties = listOf(
                TodoProperty.Title("Buy milk"),
                TodoProperty.StartDateTime(startDateTime),
                TodoProperty.EndDateTime(endDateTime)
            ),
            isCompleted = false
        )

        assertEquals("test-id-123", todo.id)
        assertEquals("Buy milk", todo.title)
        assertEquals(startDateTime, todo.startDateTime)
        assertEquals(endDateTime, todo.endDateTime)
        assertEquals(false, todo.isCompleted)
    }

    @Test
    fun todo_dataClassEquality() {
        val todo1 = Todo(
            "id1",
            listOf(
                TodoProperty.Title("Task 1"),
                TodoProperty.StartDateTime(null),
                TodoProperty.EndDateTime(null)
            ),
            false
        )
        val todo2 = Todo(
            "id1",
            listOf(
                TodoProperty.Title("Task 1"),
                TodoProperty.StartDateTime(null),
                TodoProperty.EndDateTime(null)
            ),
            false
        )
        val todo3 = Todo(
            "id2",
            listOf(
                TodoProperty.Title("Task 2"),
                TodoProperty.StartDateTime(null),
                TodoProperty.EndDateTime(null)
            ),
            true
        )

        assertEquals(todo1, todo2)
        assertNotEquals(todo1, todo3)
    }

    @Test
    fun todo_copyWithChanges() {
        val startDateTime = LocalDateTime.of(2025, 2, 12, 9, 30)
        val endDateTime = LocalDateTime.of(2025, 2, 12, 10, 45)
        val updatedStartDateTime = LocalDateTime.of(2025, 2, 13, 11, 0)
        val updatedEndDateTime = LocalDateTime.of(2025, 2, 13, 12, 0)
        val original = Todo(
            "id1",
            listOf(
                TodoProperty.Title("Original"),
                TodoProperty.StartDateTime(startDateTime),
                TodoProperty.EndDateTime(endDateTime)
            ),
            false
        )
        val completed = original.copy(isCompleted = true)
        val updated = original.copy(
            properties = listOf(
                TodoProperty.Title("Updated"),
                TodoProperty.StartDateTime(updatedStartDateTime),
                TodoProperty.EndDateTime(updatedEndDateTime)
            )
        )

        assertEquals("id1", completed.id)
        assertEquals("Original", completed.title)
        assertEquals(startDateTime, completed.startDateTime)
        assertEquals(endDateTime, completed.endDateTime)
        assertEquals(true, completed.isCompleted)

        assertEquals("id1", updated.id)
        assertEquals("Updated", updated.title)
        assertEquals(updatedStartDateTime, updated.startDateTime)
        assertEquals(updatedEndDateTime, updated.endDateTime)
        assertEquals(false, updated.isCompleted)
    }
}
