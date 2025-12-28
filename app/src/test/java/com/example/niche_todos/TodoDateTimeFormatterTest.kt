// ABOUTME: Unit tests for TodoDateTimeFormatter formatting behavior
// Verifies date/time labels for set and unset values
package com.example.niche_todos

import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.LocalDateTime

class TodoDateTimeFormatterTest {
    @Test
    fun formatLabel_formatsSetDateTime() {
        val formatter = TodoDateTimeFormatter()
        val dateTime = LocalDateTime.of(2025, 5, 1, 14, 5)

        val result = formatter.formatLabel("Start", dateTime, "Not set")

        assertEquals("Start: 2025-05-01 14:05", result)
    }

    @Test
    fun formatLabel_formatsUnsetDateTime() {
        val formatter = TodoDateTimeFormatter()

        val result = formatter.formatLabel("End", null, "Not set")

        assertEquals("End: Not set", result)
    }
}
