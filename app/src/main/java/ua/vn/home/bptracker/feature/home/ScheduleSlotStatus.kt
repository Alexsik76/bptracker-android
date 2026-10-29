package ua.vn.home.bptracker.feature.home

import java.time.Duration
import java.time.LocalTime
import ua.vn.home.bptracker.feature.reminders.TodaySlot

enum class SlotStatus {
    DONE,
    ACTIVE,
    UPCOMING,
    MISSED
}

sealed interface ActiveChipState {
    data class BeforeMinutes(val minutes: Long) : ActiveChipState
    data class BeforeHours(val hours: Long) : ActiveChipState
    data object DueNow : ActiveChipState
    data class LateMinutes(val minutes: Long) : ActiveChipState
    data class LateHoursMinutes(val hours: Long, val minutes: Long) : ActiveChipState
}

fun parseSlotTime(timeStr: String): LocalTime {
    return try {
        LocalTime.parse(timeStr.trim())
    } catch (_: Exception) {
        val parts = timeStr.trim().split(":")
        LocalTime.of(parts[0].toInt(), if (parts.size > 1) parts[1].toInt() else 0)
    }
}

fun computeSlotStatuses(
    slots: List<TodaySlot>,
    now: LocalTime,
    missedGraceMinutes: Long = 120
): List<SlotStatus> {
    if (slots.isEmpty()) return emptyList()

    val statuses = slots.mapIndexed { index, slot ->
        if (slot.taken) {
            SlotStatus.DONE
        } else {
            val slotTime = parseSlotTime(slot.time)
            val hasLaterSlot = index < slots.lastIndex
            val slotSecond = slotTime.toSecondOfDay()
            val nowSecond = now.toSecondOfDay()
            val graceSecond = missedGraceMinutes * 60
            val isMissed = hasLaterSlot && (nowSecond > slotSecond + graceSecond)
            if (isMissed) SlotStatus.MISSED else null
        }
    }

    var firstUnassignedFound = false
    return statuses.map { status ->
        when (status) {
            SlotStatus.DONE -> SlotStatus.DONE
            SlotStatus.MISSED -> SlotStatus.MISSED
            SlotStatus.ACTIVE -> SlotStatus.ACTIVE
            SlotStatus.UPCOMING -> SlotStatus.UPCOMING
            null -> {
                if (!firstUnassignedFound) {
                    firstUnassignedFound = true
                    SlotStatus.ACTIVE
                } else {
                    SlotStatus.UPCOMING
                }
            }
        }
    }
}

fun computeActiveChipState(slotTime: LocalTime, now: LocalTime): ActiveChipState {
    return if (now.isBefore(slotTime)) {
        val diffSeconds = Duration.between(now, slotTime).seconds
        val minutesBefore = (diffSeconds + 59) / 60
        if (minutesBefore < 60) {
            ActiveChipState.BeforeMinutes(minutesBefore)
        } else {
            val hours = (minutesBefore + 30) / 60
            ActiveChipState.BeforeHours(maxOf(1L, hours))
        }
    } else {
        val passedMinutes = Duration.between(slotTime, now).toMinutes()
        if (passedMinutes <= 30) {
            ActiveChipState.DueNow
        } else if (passedMinutes < 60) {
            ActiveChipState.LateMinutes(passedMinutes)
        } else {
            val hours = passedMinutes / 60
            val minutes = passedMinutes % 60
            ActiveChipState.LateHoursMinutes(hours, minutes)
        }
    }
}

fun computeActiveChipState(slotTimeString: String, now: LocalTime): ActiveChipState {
    return computeActiveChipState(parseSlotTime(slotTimeString), now)
}
