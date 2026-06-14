package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.*
import org.junit.Test
import ua.vn.home.bptracker.feature.home.ScheduleEditState

class ReminderSchedulerTest {

    @Test
    fun testTimeParsingValidation() {
        // Valid formats
        assertTrue(ScheduleEditState.Ready.validateTime("08:00"))
        assertTrue(ScheduleEditState.Ready.validateTime("23:59"))
        assertTrue(ScheduleEditState.Ready.validateTime("8:00")) // Lenient validation
        
        // Invalid formats or ranges
        assertFalse(ScheduleEditState.Ready.validateTime("24:00"))
        assertFalse(ScheduleEditState.Ready.validateTime("08:60"))
        assertFalse(ScheduleEditState.Ready.validateTime("abcd"))
    }
}
