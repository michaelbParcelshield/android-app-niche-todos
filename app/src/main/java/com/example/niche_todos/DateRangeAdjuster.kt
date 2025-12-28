// ABOUTME: Utility for keeping todo start/end date ranges valid
// Provides helpers to preserve durations when adjusting times in the UI
package com.example.niche_todos

import java.time.Duration
import java.time.LocalDateTime

object DateRangeAdjuster {
    fun shiftEndKeepingDuration(
        previousStart: LocalDateTime?,
        previousEnd: LocalDateTime?,
        newStart: LocalDateTime
    ): LocalDateTime {
        val duration = if (previousStart != null && previousEnd != null) {
            Duration.between(previousStart, previousEnd)
        } else {
            Duration.ZERO
        }
        val safeDuration = if (duration.isNegative) Duration.ZERO else duration
        return newStart.plus(safeDuration)
    }
}
