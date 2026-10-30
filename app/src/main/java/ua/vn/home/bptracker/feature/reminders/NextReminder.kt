package ua.vn.home.bptracker.feature.reminders

import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDateTime
import java.time.LocalTime

val DEFAULT_REMINDER_CONFIG = ReminderConfigDto(
    morningTime = "08:00:00",
    dayTime = "14:00:00",
    eveningTime = "20:00:00",
    maxReminders = 3,
    durationMinutes = 60
)

fun computeNextReminders(
    config: ReminderConfigDto,
    intakeConfirmed: Map<WhenSlot, Boolean>,
    now: LocalDateTime
): Map<WhenSlot, LocalDateTime> {
    return WhenSlot.entries.associateWith { slot ->
        computeNextReminder(
            slot = slot,
            config = config,
            isConfirmed = intakeConfirmed[slot] == true,
            now = now
        )
    }
}

fun computeNextReminder(
    slot: WhenSlot,
    config: ReminderConfigDto,
    isConfirmed: Boolean,
    now: LocalDateTime
): LocalDateTime {
    val timeStr = when (slot) {
        WhenSlot.Morning -> config.morningTime
        WhenSlot.Day -> config.dayTime
        WhenSlot.Evening -> config.eveningTime
    }
    val baseTime = parseSlotTime(timeStr, slot)
    val baseDateTime = LocalDateTime.of(now.toLocalDate(), baseTime)

    if (isConfirmed) {
        return baseDateTime.plusDays(1)
    }

    val candidates = mutableListOf<LocalDateTime>()
    candidates.add(baseDateTime)

    val maxReminders = config.maxReminders
    val durationMinutes = config.durationMinutes

    if (maxReminders > 0 && durationMinutes > 0) {
        for (k in 1..maxReminders) {
            val offsetMinutes = (durationMinutes.toLong() * k) / maxReminders
            candidates.add(baseDateTime.plusMinutes(offsetMinutes))
        }
    }

    return candidates.firstOrNull { it.isAfter(now) } ?: baseDateTime.plusDays(1)
}

internal fun parseSlotTime(timeStr: String?, slot: WhenSlot): LocalTime {
    if (timeStr.isNullOrBlank()) {
        return defaultSlotTime(slot)
    }
    return try {
        LocalTime.parse(timeStr)
    } catch (_: Exception) {
        defaultSlotTime(slot)
    }
}

private fun defaultSlotTime(slot: WhenSlot): LocalTime {
    return when (slot) {
        WhenSlot.Morning -> LocalTime.of(8, 0)
        WhenSlot.Day -> LocalTime.of(14, 0)
        WhenSlot.Evening -> LocalTime.of(20, 0)
    }
}
