// ABOUTME: Validates todo titles for dialog interactions
// ABOUTME: Provides helpers to determine if save actions should be enabled
package com.example.niche_todos

object TodoTitleValidator {
    fun isValid(value: CharSequence?): Boolean = normalizedTitleOrNull(value) != null

    fun normalizedTitleOrNull(value: CharSequence?): String? {
        val normalized = value?.toString()?.trim()
        return if (normalized.isNullOrEmpty()) null else normalized
    }
}
