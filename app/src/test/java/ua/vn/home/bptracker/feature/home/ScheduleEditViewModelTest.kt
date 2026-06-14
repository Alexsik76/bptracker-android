package ua.vn.home.bptracker.feature.home

import org.junit.Assert.*
import org.junit.Test
import ua.vn.home.bptracker.data.dto.PeriodConfig

class ScheduleEditViewModelTest {

    @Test
    fun testTimeValidation() {
        assertTrue(ScheduleEditState.Ready.validateTime("08:00"))
        assertTrue(ScheduleEditState.Ready.validateTime("23:59"))
        assertTrue(ScheduleEditState.Ready.validateTime("00:00"))
        
        assertFalse(ScheduleEditState.Ready.validateTime("24:00"))
        assertFalse(ScheduleEditState.Ready.validateTime("08:60"))
        assertFalse(ScheduleEditState.Ready.validateTime("8:00"))
        assertFalse(ScheduleEditState.Ready.validateTime("ab:cd"))
        assertFalse(ScheduleEditState.Ready.validateTime("123"))
    }

    @Test
    fun testReadyStateValidation() {
        val periods = mapOf("Morning" to PeriodConfig("08:00", listOf("Med1")))
        
        val validState = ScheduleEditState.Ready("id", "15", "1", periods)
        assertTrue(validState.isValid)

        assertFalse(validState.copy(durationMinutes = "0").isValid)
        assertFalse(validState.copy(durationMinutes = "-1").isValid)
        assertFalse(validState.copy(durationMinutes = "abc").isValid)
        
        assertFalse(validState.copy(maxReminders = "0").isValid)
        
        val invalidPeriods = mapOf("Morning" to PeriodConfig("25:00", listOf("Med1")))
        assertFalse(validState.copy(periods = invalidPeriods).isValid)
    }
}
