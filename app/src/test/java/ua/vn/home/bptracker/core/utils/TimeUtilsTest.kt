package ua.vn.home.bptracker.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

class TimeUtilsTest {

    @Test
    fun `parseInstantOrNull parses ISO-8601 with offset`() {
        val input = "2026-05-20T20:54:57.655+03:00"
        val expected = OffsetDateTime.parse(input).toInstant()
        val actual = TimeUtils.parseInstantOrNull(input)

        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseInstantOrNull parses ISO-8601 with Z`() {
        val input = "2026-05-20T20:54:57.655Z"
        val expected = OffsetDateTime.parse(input).toInstant()
        val actual = TimeUtils.parseInstantOrNull(input)

        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseInstantOrNull parses legacy server format with space and 4-digit offset`() {
        val input = "2026-04-22 08:22:25.118 +0300"
        val actual = TimeUtils.parseInstantOrNull(input)

        assertNotNull(actual)
        // Check that epoch millis matches the exact instant
        val expected = OffsetDateTime.parse("2026-04-22T08:22:25.118+03:00").toInstant()
        assertEquals(expected, actual)
    }

    @Test
    fun `parseInstantOrNull parses offset-less local date-time in ISO format`() {
        val input = "2026-05-20T20:54:57.655"
        val expected = LocalDateTime.parse(input).atZone(ZoneId.systemDefault()).toInstant()
        val actual = TimeUtils.parseInstantOrNull(input)

        assertNotNull(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun `parseInstantOrNull parses offset-less local date-time with space separator`() {
        val inputWithMillis = "2026-05-20 20:54:57.655"
        val expectedWithMillis = LocalDateTime.parse("2026-05-20T20:54:57.655")
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val actualWithMillis = TimeUtils.parseInstantOrNull(inputWithMillis)

        assertNotNull(actualWithMillis)
        assertEquals(expectedWithMillis, actualWithMillis)

        val inputWithoutMillis = "2026-05-20 20:54:57"
        val expectedWithoutMillis = LocalDateTime.parse("2026-05-20T20:54:57")
            .atZone(ZoneId.systemDefault())
            .toInstant()
        val actualWithoutMillis = TimeUtils.parseInstantOrNull(inputWithoutMillis)

        assertNotNull(actualWithoutMillis)
        assertEquals(expectedWithoutMillis, actualWithoutMillis)
    }

    @Test
    fun `parseInstantOrNull returns null for blank or invalid strings`() {
        assertNull(TimeUtils.parseInstantOrNull(""))
        assertNull(TimeUtils.parseInstantOrNull("   "))
        assertNull(TimeUtils.parseInstantOrNull("not-a-date"))
        assertNull(TimeUtils.parseInstantOrNull("2026-99-99"))
    }

    @Test
    fun `parseToLocal returns equivalent instant converted to system zone`() {
        val input = "2026-04-22 08:22:25.118 +0300"
        val localOdt = TimeUtils.parseToLocal(input)
        val instant = TimeUtils.parseInstantOrNull(input)

        assertNotNull(instant)
        assertEquals(instant, localOdt.toInstant())
        assertEquals(ZoneId.systemDefault().rules.getOffset(instant), localOdt.offset)
    }

    @Test
    fun `parseToLocal returns non-null fallback on invalid input`() {
        val result = TimeUtils.parseToLocal("invalid-date")
        assertNotNull(result)
    }
}
