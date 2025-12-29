// ABOUTME: Validates selected date-time values against optional minimum constraints
// Provides simple result wrapper so UI logic can react without tight coupling
package com.example.niche_todos

import java.time.LocalDateTime

object DateTimeSelectionValidator {

    sealed class ValidationResult {
        data class Valid(val dateTime: LocalDateTime) : ValidationResult()
        data class Invalid(val minimumDateTime: LocalDateTime) : ValidationResult()
    }

    fun validate(
        selection: LocalDateTime,
        minimum: LocalDateTime?
    ): ValidationResult {
        if (minimum == null) {
            return ValidationResult.Valid(selection)
        }
        return if (selection.isBefore(minimum)) {
            ValidationResult.Invalid(minimum)
        } else {
            ValidationResult.Valid(selection)
        }
    }
}
