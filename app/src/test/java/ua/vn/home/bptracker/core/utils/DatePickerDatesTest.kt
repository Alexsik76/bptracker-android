package ua.vn.home.bptracker.core.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class DatePickerDatesTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    @Before
    fun setUp() {
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    @Test
    fun `LocalDate to millis to LocalDate round trip is identity in Europe Kyiv default timezone`() {
        val testDates = listOf(
            // DST transition spring forward (Ukraine: last Sunday of March)
            LocalDate.of(2024, 3, 31),
            LocalDate.of(2025, 3, 30),
            LocalDate.of(2026, 3, 29),
            // Days around spring forward
            LocalDate.of(2026, 3, 28),
            LocalDate.of(2026, 3, 30),

            // DST transition fall back (Ukraine: last Sunday of October)
            LocalDate.of(2024, 10, 27),
            LocalDate.of(2025, 10, 26),
            LocalDate.of(2026, 10, 25),
            // Days around fall back
            LocalDate.of(2026, 10, 24),
            LocalDate.of(2026, 10, 26),

            // Winter date (standard time UTC+2)
            LocalDate.of(2026, 1, 15),

            // Summer date (DST UTC+3)
            LocalDate.of(2026, 7, 15),

            // Leap day
            LocalDate.of(2024, 2, 29),

            // Year boundaries
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1)
        )

        for (date in testDates) {
            val millis = localDateToDatePickerMillis(date)
            val roundTrip = datePickerMillisToLocalDate(millis)
            assertEquals("Round trip failed for date $date", date, roundTrip)

            // Also test via localDateToUtcMillis / utcMillisToLocalDate
            val utcMillis = localDateToUtcMillis(date)
            assertEquals(millis, utcMillis)
            val roundTripUtc = utcMillisToLocalDate(utcMillis)
            assertEquals("Round trip failed via utcMillis for date $date", date, roundTripUtc)

            // Ensure millis are exactly at UTC midnight
            assertEquals(0L, millis % (24 * 60 * 60 * 1000L))
        }
    }
}
