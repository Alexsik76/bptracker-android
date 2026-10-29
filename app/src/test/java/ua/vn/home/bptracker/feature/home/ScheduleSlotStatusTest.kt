package ua.vn.home.bptracker.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodaySlot
import java.time.LocalTime

class ScheduleSlotStatusTest {

    private fun makeSlot(
        slot: WhenSlot = WhenSlot.Morning,
        time: String = "08:00",
        taken: Boolean = false,
        takenAt: String? = null
    ) = TodaySlot(
        slot = slot,
        time = time,
        meds = emptyList(),
        taken = taken,
        takenAt = takenAt
    )

    @Test
    fun `boundary at exactly slot time yields DueNow`() {
        val slotTime = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 0)
        assertEquals(ActiveChipState.DueNow, computeActiveChipState(slotTime, now))
    }

    @Test
    fun `boundary at plus 30 minutes yields DueNow`() {
        val slotTime = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 30)
        assertEquals(ActiveChipState.DueNow, computeActiveChipState(slotTime, now))
    }

    @Test
    fun `boundary at plus 31 minutes yields LateMinutes 31`() {
        val slotTime = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 31)
        assertEquals(ActiveChipState.LateMinutes(31), computeActiveChipState(slotTime, now))
    }

    @Test
    fun `before slot time formats minutes or whole hours`() {
        val slotTime = LocalTime.of(8, 0)
        assertEquals(ActiveChipState.BeforeMinutes(15), computeActiveChipState(slotTime, LocalTime.of(7, 45)))
        assertEquals(ActiveChipState.BeforeMinutes(59), computeActiveChipState(slotTime, LocalTime.of(7, 1)))
        assertEquals(ActiveChipState.BeforeHours(1), computeActiveChipState(slotTime, LocalTime.of(7, 0)))
        assertEquals(ActiveChipState.BeforeHours(2), computeActiveChipState(slotTime, LocalTime.of(6, 0)))
    }

    @Test
    fun `late from 60 minutes yields LateHoursMinutes`() {
        val slotTime = LocalTime.of(8, 0)
        assertEquals(ActiveChipState.LateHoursMinutes(1, 0), computeActiveChipState(slotTime, LocalTime.of(9, 0)))
        assertEquals(ActiveChipState.LateHoursMinutes(1, 15), computeActiveChipState(slotTime, LocalTime.of(9, 15)))
    }

    @Test
    fun `grace edge boundary - at slot time plus grace is not missed, at plus one minute is missed`() {
        val slots = listOf(
            makeSlot(slot = WhenSlot.Morning, time = "08:00"),
            makeSlot(slot = WhenSlot.Day, time = "13:00")
        )

        // At exactly slot time + 120 minutes (10:00), morning slot is not missed
        val atGraceEdge = computeSlotStatuses(slots, LocalTime.of(10, 0), missedGraceMinutes = 120)
        assertEquals(listOf(SlotStatus.ACTIVE, SlotStatus.UPCOMING), atGraceEdge)

        // At 10:01 (1 minute past grace edge), morning slot becomes missed
        val pastGrace = computeSlotStatuses(slots, LocalTime.of(10, 1), missedGraceMinutes = 120)
        assertEquals(listOf(SlotStatus.MISSED, SlotStatus.ACTIVE), pastGrace)
    }

    @Test
    fun `last slot is never MISSED even when way past grace`() {
        val slots = listOf(
            makeSlot(slot = WhenSlot.Morning, time = "08:00", taken = true),
            makeSlot(slot = WhenSlot.Evening, time = "20:00", taken = false)
        )

        // At 23:59 (way past 20:00 + 120 min), the last slot remains ACTIVE and not MISSED
        val statuses = computeSlotStatuses(slots, LocalTime.of(23, 59))
        assertEquals(listOf(SlotStatus.DONE, SlotStatus.ACTIVE), statuses)

        // Single slot list is always the last slot, never MISSED
        val singleSlot = listOf(makeSlot(slot = WhenSlot.Morning, time = "08:00", taken = false))
        val singleResult = computeSlotStatuses(singleSlot, LocalTime.of(15, 0))
        assertEquals(listOf(SlotStatus.ACTIVE), singleResult)
    }

    @Test
    fun `all done results in no ACTIVE slot`() {
        val slots = listOf(
            makeSlot(slot = WhenSlot.Morning, time = "08:00", taken = true),
            makeSlot(slot = WhenSlot.Day, time = "13:00", taken = true),
            makeSlot(slot = WhenSlot.Evening, time = "20:00", taken = true)
        )

        val statuses = computeSlotStatuses(slots, LocalTime.of(12, 0))
        assertEquals(listOf(SlotStatus.DONE, SlotStatus.DONE, SlotStatus.DONE), statuses)
        assertFalse(statuses.contains(SlotStatus.ACTIVE))
    }

    @Test
    fun `first unconfirmed slot that is not missed is ACTIVE and remaining are UPCOMING`() {
        val slots = listOf(
            makeSlot(slot = WhenSlot.Morning, time = "08:00", taken = false),
            makeSlot(slot = WhenSlot.Day, time = "13:00", taken = false),
            makeSlot(slot = WhenSlot.Evening, time = "20:00", taken = false)
        )

        val statuses = computeSlotStatuses(slots, LocalTime.of(7, 30))
        assertEquals(listOf(SlotStatus.ACTIVE, SlotStatus.UPCOMING, SlotStatus.UPCOMING), statuses)
    }
}
