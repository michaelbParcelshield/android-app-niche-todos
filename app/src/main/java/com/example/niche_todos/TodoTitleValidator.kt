// ABOUTME: Validates todo titles for dialog interactions
// Provides helpers to determine if save actions should be enabled
package com.example.niche_todos

object TodoTitleValidator {
    fun isValid(value: CharSequence?): Boolean = !value.isNullOrBlank()
}
