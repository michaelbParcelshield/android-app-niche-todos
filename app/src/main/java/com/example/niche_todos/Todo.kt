// ABOUTME: Todo data class representing a single todo item
// Contains id, text description, and completion status
package com.example.niche_todos

data class Todo(
    val id: String,
    val text: String,
    val isCompleted: Boolean
)
