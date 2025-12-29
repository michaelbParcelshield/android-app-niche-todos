// ABOUTME: Tests for ensuring start/end adjustments preserve durations and validity
// Verifies shifting logic used by UI when users update start or end times
package com.example.niche_todos

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DateRangeAdjusterTest {
    @Test
    fun shiftEndKeepsOriginalDuration() {
        val previousStart = LocalDateTime.of(2025, 6, 1, 9, 0)
        val previousEnd = LocalDateTime.of(2025, 6, 1, 17, 30)
        val newStart = LocalDateTime.of(2025, 6, 1, 10, 15)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(previousStart, previousEnd, newStart)

        val expectedEnd = LocalDateTime.of(2025, 6, 1, 18, 45)
        assertEquals(expectedEnd, adjustedEnd)
    }

    @Test
    fun shiftEndHandlesMissingPreviousEnd() {
        val previousStart = LocalDateTime.of(2025, 6, 2, 8, 0)
        val newStart = LocalDateTime.of(2025, 6, 2, 9, 45)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(previousStart, null, newStart)

        assertEquals(newStart, adjustedEnd)
    }

    @Test
    fun shiftEndTreatsNegativeDurationAsZero() {
        val previousStart = LocalDateTime.of(2025, 6, 3, 12, 0)
        val previousEnd = LocalDateTime.of(2025, 6, 3, 11, 0)
        val newStart = LocalDateTime.of(2025, 6, 3, 14, 0)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(previousStart, previousEnd, newStart)

        assertEquals(newStart, adjustedEnd)
    }

    @Test
    fun shiftEndKeepsExistingEndWhenStartMissing() {
        val previousEnd = LocalDateTime.of(2025, 7, 4, 18, 0)
        val newStart = LocalDateTime.of(2025, 7, 4, 9, 0)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(null, previousEnd, newStart)

        assertEquals(previousEnd, adjustedEnd)
    }

    @Test
    fun shiftEndAlignsWithNewStartWhenExistingEndBeforeNewStart() {
        val previousEnd = LocalDateTime.of(2025, 7, 4, 8, 30)
        val newStart = LocalDateTime.of(2025, 7, 4, 9, 0)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(null, previousEnd, newStart)

        assertEquals(newStart, adjustedEnd)
    }

    @Test
    fun shiftEndDefaultsToNewStartWhenNoPreviousDatesExist() {
        val newStart = LocalDateTime.of(2025, 8, 15, 7, 30)

        val adjustedEnd = DateRangeAdjuster.shiftEndKeepingDuration(null, null, newStart)

        assertEquals(newStart, adjustedEnd)
    }
}
