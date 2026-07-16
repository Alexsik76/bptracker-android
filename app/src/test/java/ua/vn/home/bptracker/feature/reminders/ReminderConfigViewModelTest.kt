package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.*
import org.junit.Test

class ReminderConfigViewModelTest {

    @Test
    fun testStateValidation() {
        val validState = ReminderConfigState(
            maxReminders = "3",
            durationMinutes = "60"
        )
        assertTrue(validState.isValid)

        assertFalse(validState.copy(maxReminders = "0").isValid)
        assertFalse(validState.copy(maxReminders = "abc").isValid)
        assertFalse(validState.copy(durationMinutes = "0").isValid)
        assertFalse(validState.copy(durationMinutes = "").isValid)
    }

    @Test
    fun testTimeParsing() {
        val state = ReminderConfigState(
            morningTime = "07:30:00",
            dayTime = "13:45:00",
            eveningTime = "21:15:00"
        )
        
        assertEquals(7, state.morningHour)
        assertEquals(30, state.morningMinute)
        
        assertEquals(13, state.dayHour)
        assertEquals(45, state.dayMinute)
        
        assertEquals(21, state.eveningHour)
        assertEquals(15, state.eveningMinute)
    }

    @Test
    fun testMalformedTimeParsing() {
        val state = ReminderConfigState(morningTime = "invalid")
        // Should fallback to default 8:00
        assertEquals(8, state.morningHour)
        assertEquals(0, state.morningMinute)
    }
}
