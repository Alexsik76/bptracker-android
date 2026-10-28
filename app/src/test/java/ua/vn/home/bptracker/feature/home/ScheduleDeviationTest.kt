package ua.vn.home.bptracker.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleDeviationTest {

    private val scheduleDate = "2026-10-27"
    private val slotTime = "08:00"

    @Test
    fun `returns vchasno when taken exactly on time`() {
        val result = calculateScheduleDeviation(
            scheduleDate = scheduleDate,
            slotTime = slotTime,
            takenAt = "2026-10-27T08:00:00"
        )
        assertEquals("вчасно", result)
    }

    @Test
    fun `returns vchasno within plus or minus 5 minutes`() {
        assertEquals(
            "вчасно",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T08:05:00")
        )
        assertEquals(
            "вчасно",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T08:03:00")
        )
        assertEquals(
            "вчасно",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:55:00")
        )
        assertEquals(
            "вчасно",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:58:00")
        )
    }

    @Test
    fun `returns signed positive minutes when late by more than 5 minutes`() {
        assertEquals(
            "+6 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T08:06:00")
        )
        assertEquals(
            "+20 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T08:20:00")
        )
        assertEquals(
            "+59 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T08:59:00")
        )
    }

    @Test
    fun `returns signed negative minutes when early by more than 5 minutes`() {
        assertEquals(
            "−6 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:54:00")
        )
        assertEquals(
            "−15 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:45:00")
        )
        assertEquals(
            "−59 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:01:00")
        )
    }

    @Test
    fun `formats hours and minutes when delta is 60 minutes or more`() {
        assertEquals(
            "+1 год",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T09:00:00")
        )
        assertEquals(
            "+1 год 5 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T09:05:00")
        )
        assertEquals(
            "+2 год 15 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T10:15:00")
        )
        assertEquals(
            "−1 год",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T07:00:00")
        )
        assertEquals(
            "−1 год 15 хв",
            calculateScheduleDeviation(scheduleDate, slotTime, "2026-10-27T06:45:00")
        )
    }

    @Test
    fun `supports custom localized strings`() {
        assertEquals(
            "on time",
            calculateScheduleDeviation(
                scheduleDate, slotTime, "2026-10-27T08:04:00",
                onTimeLabel = "on time", minuteUnit = "min", hourUnit = "h"
            )
        )
        assertEquals(
            "+20 min",
            calculateScheduleDeviation(
                scheduleDate, slotTime, "2026-10-27T08:20:00",
                onTimeLabel = "on time", minuteUnit = "min", hourUnit = "h"
            )
        )
        assertEquals(
            "+1 h 5 min",
            calculateScheduleDeviation(
                scheduleDate, slotTime, "2026-10-27T09:05:00",
                onTimeLabel = "on time", minuteUnit = "min", hourUnit = "h"
            )
        )
        assertEquals(
            "−15 min",
            calculateScheduleDeviation(
                scheduleDate, slotTime, "2026-10-27T07:45:00",
                onTimeLabel = "on time", minuteUnit = "min", hourUnit = "h"
            )
        )
    }

    @Test
    fun `returns null for missing or invalid inputs`() {
        assertNull(calculateScheduleDeviation(scheduleDate, slotTime, null))
        assertNull(calculateScheduleDeviation(scheduleDate, slotTime, ""))
        assertNull(calculateScheduleDeviation(scheduleDate, slotTime, "   "))
        assertNull(calculateScheduleDeviation("", slotTime, "2026-10-27T08:00:00"))
        assertNull(calculateScheduleDeviation(scheduleDate, "", "2026-10-27T08:00:00"))
    }
}
