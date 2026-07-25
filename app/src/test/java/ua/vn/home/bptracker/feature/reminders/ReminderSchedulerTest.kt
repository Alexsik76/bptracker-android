package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class ReminderSchedulerTest {

    @Test
    fun testTimeParsingValidation() {
        // We verify that the format used by ReminderScheduler (HH:mm:ss or HH:mm) is parsable
        val validTime = "08:00:00"
        val parsed = try {
            LocalTime.parse(validTime)
        } catch (e: Exception) {
            null
        }
        assertNotNull(parsed)
        assertEquals(8, parsed?.hour)
        assertEquals(0, parsed?.minute)

        // Invalid format
        val invalidTime = "25:00"
        val parsedInvalid = try {
            LocalTime.parse(invalidTime)
        } catch (e: Exception) {
            null
        }
        assertNull(parsedInvalid)
    }
}
