// ABOUTME: Converts between local date/time values and UTC ISO-8601 strings.
// ABOUTME: Ensures backend storage uses UTC while UI stays in local time.
package com.example.niche_todos

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object TodoUtcConverter {
    private val localZone: ZoneId = ZoneId.systemDefault()

    fun toUtcString(localDateTime: LocalDateTime?): String? {
        return localDateTime
            ?.atZone(localZone)
            ?.withZoneSameInstant(ZoneOffset.UTC)
            ?.toInstant()
            ?.toString()
    }

    fun fromUtcString(utcValue: String?): LocalDateTime? {
        return utcValue?.let { Instant.parse(it).atZone(localZone).toLocalDateTime() }
    }
}
