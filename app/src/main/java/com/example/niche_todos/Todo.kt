// ABOUTME: Todo data class representing a single todo item and its properties
// Defines property types for title and date/time values plus completion status
package com.example.niche_todos

import java.time.LocalDateTime

sealed class TodoProperty {
    data class Title(val value: String) : TodoProperty()
    data class StartDateTime(val value: LocalDateTime?) : TodoProperty()
    data class EndDateTime(val value: LocalDateTime?) : TodoProperty()
}

data class Todo(
    val id: String,
    val properties: List<TodoProperty>,
    val isCompleted: Boolean,
    val parentId: String? = null,
    val sortOrder: Int = 0
) {
    val title: String
        get() = properties.filterIsInstance<TodoProperty.Title>().firstOrNull()?.value ?: ""

    val startDateTime: LocalDateTime?
        get() = properties.filterIsInstance<TodoProperty.StartDateTime>().firstOrNull()?.value

    val endDateTime: LocalDateTime?
        get() = properties.filterIsInstance<TodoProperty.EndDateTime>().firstOrNull()?.value
}
