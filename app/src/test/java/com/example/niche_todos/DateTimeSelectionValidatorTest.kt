// ABOUTME: Tests for validating date-time selections relative to minimum constraints
// Ensures picker logic can detect invalid user selections deterministically
package com.example.niche_todos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DateTimeSelectionValidatorTest {

    @Test
    fun validationPassesWhenSelectionMeetsMinimum() {
        val minDateTime = LocalDateTime.of(2025, 2, 3, 10, 0)
        val selection = LocalDateTime.of(2025, 2, 3, 10, 30)

        val result = DateTimeSelectionValidator.validate(selection, minDateTime)

        assertTrue(result is DateTimeSelectionValidator.ValidationResult.Valid)
        if (result is DateTimeSelectionValidator.ValidationResult.Valid) {
            assertEquals(selection, result.dateTime)
        }
    }

    @Test
    fun validationRejectsWhenSelectionBeforeMinimum() {
        val minDateTime = LocalDateTime.of(2025, 3, 4, 9, 15)
        val selection = LocalDateTime.of(2025, 3, 4, 8, 0)

        val result = DateTimeSelectionValidator.validate(selection, minDateTime)

        assertTrue(result is DateTimeSelectionValidator.ValidationResult.Invalid)
        if (result is DateTimeSelectionValidator.ValidationResult.Invalid) {
            assertEquals(minDateTime, result.minimumDateTime)
        }
    }
}
