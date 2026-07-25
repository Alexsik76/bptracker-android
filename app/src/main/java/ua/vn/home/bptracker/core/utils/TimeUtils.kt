package ua.vn.home.bptracker.core.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    fun parseToLocal(recordedAt: String): OffsetDateTime {
        if (recordedAt.isBlank()) return OffsetDateTime.now()
        
        val input = recordedAt.trim()
        
        return try {
            // Precise match for: "2026-04-22 08:22:25.118 +0300"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z")
            ZonedDateTime.parse(input, formatter)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toOffsetDateTime()
        } catch (_: Exception) {
            try {
                // Fallback for ISO format: "2024-05-20T20:54:57.655+03:00"
                OffsetDateTime.parse(input)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toOffsetDateTime()
            } catch (_: Exception) {
                try {
                    // Fallback for space-separated ISO without full offset: "2024-05-20 20:54:57"
                    val sanitized = input.replace(" ", "T")
                    LocalDateTime.parse(sanitized)
                        .atZone(ZoneId.systemDefault())
                        .toOffsetDateTime()
                } catch (_: Exception) {
                    OffsetDateTime.now()
                }
            }
        }
    }
}
