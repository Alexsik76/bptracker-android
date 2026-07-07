package ua.vn.home.bptracker.core.utils

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {
    fun parseToLocal(recordedAt: String): OffsetDateTime {
        if (recordedAt.isBlank()) return OffsetDateTime.now()
        
        // Log for debugging if needed (removed for production)
        val input = recordedAt.trim()
        
        return try {
            // 1. Try format: "2024-05-20 20:54:57.655 +0300"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z")
            ZonedDateTime.parse(input, formatter)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toOffsetDateTime()
        } catch (e: Exception) {
            try {
                // 2. Try standard ISO: "2024-05-20T20:54:57.655+03:00"
                OffsetDateTime.parse(input)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toOffsetDateTime()
            } catch (e2: Exception) {
                try {
                    // 3. Try format with T but no colon in offset: "2024-05-20T20:54:57.655+0300"
                    val isoNoColonFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    ZonedDateTime.parse(input.replace(" ", "T"), isoNoColonFormatter)
                        .withZoneSameInstant(ZoneId.systemDefault())
                        .toOffsetDateTime()
                } catch (e3: Exception) {
                    // Fallback: If it contains a space but isn't matching above, try to sanitize it to ISO
                    try {
                        // "2024-05-20 20:54:57" -> ISO
                        val sanitized = input.replace(" ", "T")
                        OffsetDateTime.parse(sanitized)
                            .atZoneSameInstant(ZoneId.systemDefault())
                            .toOffsetDateTime()
                    } catch (e4: Exception) {
                        OffsetDateTime.now()
                    }
                }
            }
        }
    }
}
