package ua.vn.home.bptracker.core.utils

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    fun parseToLocal(recordedAt: String): OffsetDateTime {
        if (recordedAt.isBlank()) return OffsetDateTime.now()
        
        return try {
            // Try ISO format with T (2024-05-20T20:54:57.655+03:00)
            OffsetDateTime.parse(recordedAt.replace(" ", "T"))
                .atZoneSameInstant(ZoneId.systemDefault())
                .toOffsetDateTime()
        } catch (e: Exception) {
            try {
                // Try parsing with space and offset (2024-05-20 20:54:57.655 +0300)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z")
                ZonedDateTime.parse(recordedAt, formatter)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toOffsetDateTime()
            } catch (e2: Exception) {
                try {
                    // Try another common format if space is present but offset is Z
                    val cleaned = recordedAt.replace(" ", "T")
                    OffsetDateTime.parse(cleaned)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toOffsetDateTime()
                } catch (e3: Exception) {
                    // Fallback to now
                    OffsetDateTime.now()
                }
            }
        }
    }
}
