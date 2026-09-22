package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDate

class NotificationEventKeyTest {

    @Test
    fun `event keys and derived IDs are distinct for all slots across consecutive days`() {
        val day1 = LocalDate.of(2026, 7, 16)
        val day2 = LocalDate.of(2026, 7, 17)
        val days = listOf(day1, day2)
        val slots = listOf(WhenSlot.Morning, WhenSlot.Day, WhenSlot.Evening)

        val keys = mutableListOf<Int>()
        val notificationIds = mutableListOf<Int>()
        val contentRequestCodes = mutableListOf<Int>()
        val actionRequestCodes = mutableListOf<Int>()

        for (day in days) {
            for (slot in slots) {
                val key = NotificationHelper.eventKey(day, slot)
                keys.add(key)
                notificationIds.add(key * 4 + 0)
                contentRequestCodes.add(key * 4 + 1)
                actionRequestCodes.add(key * 4 + 2)
            }
        }

        assertEquals(6, keys.toSet().size)

        val allIds = notificationIds + contentRequestCodes + actionRequestCodes
        assertEquals(18, allIds.toSet().size)
    }
}
