package ua.vn.home.bptracker.core.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    private val LEGACY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z")
    private val LOCAL_SPACE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss][.SSS]")

    fun parseInstantOrNull(s: String): Instant? {
        if (s.isBlank()) return null
        val input = s.trim()

        // 1. Try ISO-8601 with offset or Z
        try {
            return OffsetDateTime.parse(input).toInstant()
        } catch (_: Exception) {}

        // 2. Try legacy server format "yyyy-MM-dd HH:mm:ss.SSS Z"
        try {
            return ZonedDateTime.parse(input, LEGACY_FORMATTER).toInstant()
        } catch (_: Exception) {}

        // 3. Try offset-less local date-time (ISO format)
        try {
            return LocalDateTime.parse(input).atZone(ZoneId.systemDefault()).toInstant()
        } catch (_: Exception) {}

        // 4. Try offset-less local date-time (space-separated)
        try {
            return LocalDateTime.parse(input, LOCAL_SPACE_FORMATTER).atZone(ZoneId.systemDefault()).toInstant()
        } catch (_: Exception) {}

        return null
    }

    fun parseToLocal(recordedAt: String): OffsetDateTime {
        val instant = parseInstantOrNull(recordedAt) ?: return OffsetDateTime.now()
        return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime()
    }
}
