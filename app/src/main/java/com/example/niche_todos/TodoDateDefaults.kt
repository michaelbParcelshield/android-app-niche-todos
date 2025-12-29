// ABOUTME: Shared date constants and helpers for todo start/end defaults
// Centralizes end-of-day time so UI and ViewModel stay consistent
package com.example.niche_todos

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object TodoDateDefaults {
    val END_OF_DAY_TIME: LocalTime = LocalTime.of(23, 59)

    fun endOfDay(date: LocalDate): LocalDateTime = LocalDateTime.of(date, END_OF_DAY_TIME)
}
