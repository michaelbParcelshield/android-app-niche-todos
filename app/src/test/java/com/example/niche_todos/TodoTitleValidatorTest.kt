// ABOUTME: Unit tests for TodoTitleValidator
// ABOUTME: Ensures todo title validation only passes for non-blank values
package com.example.niche_todos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoTitleValidatorTest {
    @Test
    fun isValid_blankInput_returnsFalse() {
        assertFalse(TodoTitleValidator.isValid(null))
        assertFalse(TodoTitleValidator.isValid(""))
        assertFalse(TodoTitleValidator.isValid("   "))
        assertFalse(TodoTitleValidator.isValid("\t"))
        assertFalse(TodoTitleValidator.isValid("\n"))
        assertFalse(TodoTitleValidator.isValid("\u00A0"))
    }

    @Test
    fun isValid_nonBlankInput_returnsTrue() {
        assertTrue(TodoTitleValidator.isValid("Task"))
        assertTrue(TodoTitleValidator.isValid("  Task  "))
    }

    @Test
    fun normalizedTitleOrNull_blankInput_returnsNull() {
        assertNull(TodoTitleValidator.normalizedTitleOrNull(""))
        assertNull(TodoTitleValidator.normalizedTitleOrNull("   "))
    }

    @Test
    fun normalizedTitleOrNull_trimsWhitespaceBeforeReturning() {
        val normalized = TodoTitleValidator.normalizedTitleOrNull("  Task  ")
        assertEquals("Task", normalized)
    }
}
