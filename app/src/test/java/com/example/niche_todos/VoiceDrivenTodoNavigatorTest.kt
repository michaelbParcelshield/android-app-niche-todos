// ABOUTME: Unit tests for voice-driven todo navigation helper.
// ABOUTME: Ensures top-level unchecked selection, wraparound, and nested ignoring.
package com.example.niche_todos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceDrivenTodoNavigatorTest {

    @Test
    fun nextTopLevelUnchecked_returnsFirstWhenLastNull() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = false, parentId = null),
            todo(id = "b", title = "B", completed = false, parentId = null)
        )
        assertEquals("a", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, null)?.id)
    }

    @Test
    fun nextTopLevelUnchecked_ignoresNestedTodos() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = false, parentId = null),
            todo(id = "a1", title = "A1", completed = false, parentId = "a"),
            todo(id = "b", title = "B", completed = false, parentId = null)
        )
        assertEquals("a", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, null)?.id)
        assertEquals("b", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, "a")?.id)
    }

    @Test
    fun nextTopLevelUnchecked_wrapsToStartAfterBottom() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = false, parentId = null),
            todo(id = "b", title = "B", completed = false, parentId = null),
            todo(id = "c", title = "C", completed = false, parentId = null)
        )
        assertEquals("a", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, "c")?.id)
    }

    @Test
    fun nextTopLevelUnchecked_skipsCompletedTopLevelItems() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = true, parentId = null),
            todo(id = "b", title = "B", completed = false, parentId = null),
            todo(id = "c", title = "C", completed = false, parentId = null)
        )
        assertEquals("b", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, null)?.id)
        assertEquals("c", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, "b")?.id)
    }

    @Test
    fun nextTopLevelUnchecked_returnsNullWhenAllTopLevelCompleted() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = true, parentId = null),
            todo(id = "b", title = "B", completed = true, parentId = null),
            todo(id = "a1", title = "A1", completed = false, parentId = "a")
        )
        assertNull(VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, null))
    }

    @Test
    fun nextTopLevelUnchecked_whenLastIdNotFound_returnsFirstCandidate() {
        val todos = listOf(
            todo(id = "a", title = "A", completed = false, parentId = null),
            todo(id = "b", title = "B", completed = false, parentId = null)
        )
        assertEquals("a", VoiceDrivenTodoNavigator.nextTopLevelUnchecked(todos, "missing")?.id)
    }

    private fun todo(id: String, title: String, completed: Boolean, parentId: String?): Todo {
        return Todo(
            id = id,
            properties = listOf(TodoProperty.Title(title)),
            isCompleted = completed,
            parentId = parentId,
            sortOrder = 0
        )
    }
}

