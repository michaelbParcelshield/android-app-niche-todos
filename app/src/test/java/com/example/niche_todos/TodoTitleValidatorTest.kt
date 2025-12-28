// ABOUTME: Unit tests for TodoTitleValidator
// Ensures todo title validation only passes for non-blank values
package com.example.niche_todos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoTitleValidatorTest {
    @Test
    fun isValid_blankInput_returnsFalse() {
        assertFalse(TodoTitleValidator.isValid(null))
        assertFalse(TodoTitleValidator.isValid(""))
        assertFalse(TodoTitleValidator.isValid("   "))
    }

    @Test
    fun isValid_nonBlankInput_returnsTrue() {
        assertTrue(TodoTitleValidator.isValid("Task"))
        assertTrue(TodoTitleValidator.isValid("  Task  "))
    }
}
