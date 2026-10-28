package ua.vn.home.bptracker.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.local.entity.SyncState

class MigrationConversionTest {

    private val migrationTimeMs = 1750000000000L

    @Test
    fun `convertMeasurementRowForMigration with valid ISO string preserves row and converts timestamp`() {
        val iso = "2026-05-20T20:54:57.655+03:00"
        val expectedMs = TimeUtils.parseInstantOrNull(iso)!!.toEpochMilli()

        val converted = convertMeasurementRowForMigration(
            id = "row-1",
            recordedAt = iso,
            sys = 120,
            dia = 80,
            pulse = 70,
            syncState = SyncState.SYNCED,
            migrationTimeMs = migrationTimeMs
        )

        assertNotNull(converted)
        assertEquals("row-1", converted?.id)
        assertEquals(expectedMs, converted?.recordedAtMs)
        assertEquals(120, converted?.sys)
        assertEquals(80, converted?.dia)
        assertEquals(70, converted?.pulse)
        assertEquals(SyncState.SYNCED, converted?.syncState)
    }

    @Test
    fun `convertMeasurementRowForMigration with valid legacy format preserves row and converts timestamp`() {
        val legacy = "2026-04-22 08:22:25.118 +0300"
        val expectedMs = TimeUtils.parseInstantOrNull(legacy)!!.toEpochMilli()

        val converted = convertMeasurementRowForMigration(
            id = "row-legacy",
            recordedAt = legacy,
            sys = 135,
            dia = 85,
            pulse = 72,
            syncState = SyncState.SYNCED,
            migrationTimeMs = migrationTimeMs
        )

        assertNotNull(converted)
        assertEquals("row-legacy", converted?.id)
        assertEquals(expectedMs, converted?.recordedAtMs)
        assertEquals(135, converted?.sys)
        assertEquals(85, converted?.dia)
        assertEquals(72, converted?.pulse)
        assertEquals(SyncState.SYNCED, converted?.syncState)
    }

    @Test
    fun `convertMeasurementRowForMigration drops unparseable SYNCED rows`() {
        val converted = convertMeasurementRowForMigration(
            id = "bad-synced",
            recordedAt = "unparseable-date",
            sys = 130,
            dia = 85,
            pulse = 75,
            syncState = SyncState.SYNCED,
            migrationTimeMs = migrationTimeMs
        )

        assertNull(converted)
    }

    @Test
    fun `convertMeasurementRowForMigration keeps unparseable PENDING_CREATE rows with migration time`() {
        val converted = convertMeasurementRowForMigration(
            id = "bad-pending-create",
            recordedAt = "corrupt-data",
            sys = 140,
            dia = 90,
            pulse = 80,
            syncState = SyncState.PENDING_CREATE,
            migrationTimeMs = migrationTimeMs
        )

        assertNotNull(converted)
        assertEquals("bad-pending-create", converted?.id)
        assertEquals(migrationTimeMs, converted?.recordedAtMs)
        assertEquals(140, converted?.sys)
        assertEquals(90, converted?.dia)
        assertEquals(80, converted?.pulse)
        assertEquals(SyncState.PENDING_CREATE, converted?.syncState)
    }

    @Test
    fun `convertMeasurementRowForMigration keeps unparseable PENDING_DELETE rows with migration time`() {
        val converted = convertMeasurementRowForMigration(
            id = "bad-pending-delete",
            recordedAt = "",
            sys = 110,
            dia = 70,
            pulse = 65,
            syncState = SyncState.PENDING_DELETE,
            migrationTimeMs = migrationTimeMs
        )

        assertNotNull(converted)
        assertEquals("bad-pending-delete", converted?.id)
        assertEquals(migrationTimeMs, converted?.recordedAtMs)
        assertEquals(110, converted?.sys)
        assertEquals(70, converted?.dia)
        assertEquals(65, converted?.pulse)
        assertEquals(SyncState.PENDING_DELETE, converted?.syncState)
    }
}
