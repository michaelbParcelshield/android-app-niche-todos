// ABOUTME: Formats todo date/time values for display in the list UI
// Produces labeled strings with a consistent date/time pattern
package com.example.niche_todos

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoDateTimeFormatter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun formatLabel(label: String, dateTime: LocalDateTime?, notSetLabel: String): String {
        val formatted = dateTime?.format(formatter) ?: notSetLabel
        return "$label: $formatted"
    }
}
