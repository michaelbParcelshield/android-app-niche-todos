// ABOUTME: Serializable API models for todo CRUD and ordering requests.
// ABOUTME: Mirrors backend payloads while keeping date values as UTC strings.
package com.example.niche_todos

import kotlinx.serialization.Serializable

@Serializable
data class TodoPayload(
    val id: String,
    val title: String,
    val startDateTimeUtc: String? = null,
    val endDateTimeUtc: String? = null,
    val isCompleted: Boolean,
    val sortOrder: Int
)

@Serializable
data class CreateTodoRequest(
    val title: String,
    val startDateTimeUtc: String? = null,
    val endDateTimeUtc: String? = null,
    val isCompleted: Boolean
)

@Serializable
data class UpdateTodoRequest(
    val title: String,
    val startDateTimeUtc: String? = null,
    val endDateTimeUtc: String? = null,
    val isCompleted: Boolean
)

@Serializable
data class ReorderTodosRequest(
    val orderedIds: List<String>
)
