package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDate
import java.time.LocalDateTime

class NextReminderTest {

    private val testDate = LocalDate.of(2026, 10, 30)

    // Morning base 08:00, repeats at 08:20 (k=1) and 08:40 (k=2)
    private val configWithTwoRepeats = ReminderConfigDto(
        morningTime = "08:00:00",
        dayTime = "14:00:00",
        eveningTime = "20:00:00",
        maxReminders = 2,
        durationMinutes = 40
    )

    @Test
    fun `opening app at 08-10 with unconfirmed morning dose returns first repeat at 08-20 today`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 10, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configWithTwoRepeats,
            isConfirmed = false,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 20, 0)), next)
    }

    @Test
    fun `opening app at 08-25 with unconfirmed morning dose returns second repeat at 08-40 today`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 25, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configWithTwoRepeats,
            isConfirmed = false,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 40, 0)), next)
    }

    @Test
    fun `confirmed slot moves to next day`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 10, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configWithTwoRepeats,
            isConfirmed = true,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate.plusDays(1), java.time.LocalTime.of(8, 0, 0)), next)
    }

    @Test
    fun `time after last repeat moves to next day`() {
        // Last repeat was 08:40, current time is 08:45
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 45, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configWithTwoRepeats,
            isConfirmed = false,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate.plusDays(1), java.time.LocalTime.of(8, 0, 0)), next)
    }

    @Test
    fun `time before base time returns base time today`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(7, 30, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configWithTwoRepeats,
            isConfirmed = false,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 0, 0)), next)
    }

    @Test
    fun `zero max reminders moves to next day after base time`() {
        val configZeroRepeats = configWithTwoRepeats.copy(maxReminders = 0, durationMinutes = 0)
        val beforeBase = LocalDateTime.of(testDate, java.time.LocalTime.of(7, 30, 0))
        val afterBase = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 10, 0))

        val nextBefore = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configZeroRepeats,
            isConfirmed = false,
            now = beforeBase
        )
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 0, 0)), nextBefore)

        val nextAfter = computeNextReminder(
            slot = WhenSlot.Morning,
            config = configZeroRepeats,
            isConfirmed = false,
            now = afterBase
        )
        assertEquals(LocalDateTime.of(testDate.plusDays(1), java.time.LocalTime.of(8, 0, 0)), nextAfter)
    }

    @Test
    fun `computeNextReminders computes correct alarm for all slots`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 10, 0))
        val intakeState = mapOf(
            WhenSlot.Morning to false,
            WhenSlot.Day to false,
            WhenSlot.Evening to false
        )

        val results = computeNextReminders(
            config = configWithTwoRepeats,
            intakeConfirmed = intakeState,
            now = now
        )

        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 20, 0)), results[WhenSlot.Morning])
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(14, 0, 0)), results[WhenSlot.Day])
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(20, 0, 0)), results[WhenSlot.Evening])
    }

    @Test
    fun `computeNextReminders respects confirmation state for Morning`() {
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(8, 10, 0))
        val intakeState = mapOf(
            WhenSlot.Morning to true,
            WhenSlot.Day to false,
            WhenSlot.Evening to false
        )

        val results = computeNextReminders(
            config = configWithTwoRepeats,
            intakeConfirmed = intakeState,
            now = now
        )

        assertEquals(LocalDateTime.of(testDate.plusDays(1), java.time.LocalTime.of(8, 0, 0)), results[WhenSlot.Morning])
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(14, 0, 0)), results[WhenSlot.Day])
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(20, 0, 0)), results[WhenSlot.Evening])
    }

    @Test
    fun `invalid time string falls back to default slot time`() {
        val badConfig = ReminderConfigDto(
            morningTime = "invalid-time",
            dayTime = "14:00:00",
            eveningTime = "20:00:00",
            maxReminders = 1,
            durationMinutes = 30
        )
        val now = LocalDateTime.of(testDate, java.time.LocalTime.of(7, 0, 0))
        val next = computeNextReminder(
            slot = WhenSlot.Morning,
            config = badConfig,
            isConfirmed = false,
            now = now
        )
        assertEquals(LocalDateTime.of(testDate, java.time.LocalTime.of(8, 0, 0)), next)
    }
}
