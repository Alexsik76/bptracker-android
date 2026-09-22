package ua.vn.home.bptracker.data.repository

import androidx.room.withTransaction
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.dto.MeasurementPageDto
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.SyncState
import java.util.UUID

import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.data.local.dao.HealthConnectExportDao

class RealMeasurementRepositoryTest {

    private val db = mockk<BpDatabase>()
    private val api = mockk<MeasurementApi>()
    private val dao = mockk<MeasurementDao>(relaxed = true)
    private val exportDao = mockk<HealthConnectExportDao>(relaxed = true)
    private val settingsStore = mockk<SettingsStore>(relaxed = true)
    
    private val storage = mutableMapOf<String, MeasurementEntity>()
    private lateinit var repository: RealMeasurementRepository

    @Before
    fun setup() {
        storage.clear()
        
        // Mock DAO to use local storage map
        coEvery { dao.getAll() } answers { storage.values.filter { it.syncState != SyncState.PENDING_DELETE } }
        coEvery { dao.insert(any()) } answers { 
            val entity = it.invocation.args[0] as MeasurementEntity
            storage[entity.id] = entity 
        }
        coEvery { dao.insertAll(any()) } answers {
            val list = it.invocation.args[0] as List<MeasurementEntity>
            list.forEach { entity -> storage[entity.id] = entity }
        }
        coEvery { dao.deleteById(any()) } answers { storage.remove(it.invocation.args[0] as String) }
        coEvery { dao.getPending() } answers { storage.values.filter { it.syncState != SyncState.SYNCED } }
        coEvery { dao.getAbsentSyncedIds(any(), any()) } answers {
            val windowStart = it.invocation.args[0] as String
            val remoteIds = (it.invocation.args[1] as List<*>).filterIsInstance<String>().toSet()
            storage.values.filter { entity ->
                entity.syncState == SyncState.SYNCED &&
                    entity.recordedAt >= windowStart &&
                    !remoteIds.contains(entity.id)
            }.map { it.id }
        }
        coEvery { dao.deleteAbsentSynced(any(), any()) } answers {
            val windowStart = it.invocation.args[0] as String
            val remoteIds = (it.invocation.args[1] as List<*>).filterIsInstance<String>().toSet()
            storage.entries.removeIf { entry ->
                val entity = entry.value
                entity.syncState == SyncState.SYNCED &&
                    entity.recordedAt >= windowStart &&
                    !remoteIds.contains(entity.id)
            }
        }
        coEvery { dao.getAbsentSyncedIdsInRange(any(), any(), any()) } answers {
            val dateFrom = it.invocation.args[0] as? String
            val dateTo = it.invocation.args[1] as? String
            val remoteIds = (it.invocation.args[2] as List<*>).filterIsInstance<String>().toSet()
            storage.values.filter { entity ->
                val matchFrom = dateFrom == null || entity.recordedAt >= dateFrom
                val matchTo = dateTo == null || entity.recordedAt < dateTo
                entity.syncState == SyncState.SYNCED &&
                    matchFrom && matchTo &&
                    !remoteIds.contains(entity.id)
            }.map { it.id }
        }
        coEvery { dao.deleteAbsentSyncedInRange(any(), any(), any()) } answers {
            val dateFrom = it.invocation.args[0] as? String
            val dateTo = it.invocation.args[1] as? String
            val remoteIds = (it.invocation.args[2] as List<*>).filterIsInstance<String>().toSet()
            storage.entries.removeIf { entry ->
                val entity = entry.value
                val matchFrom = dateFrom == null || entity.recordedAt >= dateFrom
                val matchTo = dateTo == null || entity.recordedAt < dateTo
                entity.syncState == SyncState.SYNCED &&
                    matchFrom && matchTo &&
                    !remoteIds.contains(entity.id)
            }
        }
        coEvery { dao.markPendingDelete(any()) } answers {
            val id = it.invocation.args[0] as String
            storage[id] = storage[id]!!.copy(syncState = SyncState.PENDING_DELETE)
        }

        // Mock withTransaction
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionBlock = slot<suspend () -> Any>()
        coEvery { db.withTransaction(capture(transactionBlock)) } coAnswers {
            transactionBlock.captured.invoke()
        }

        repository = RealMeasurementRepository(db, api, dao, exportDao, settingsStore)
    }

    @Test
    fun `createMeasurement creates PENDING_CREATE row and launches sync`() = runBlocking {
        val serverDto = MeasurementDto("server_id", "now", 130, 85, 75)
        coEvery { api.createMeasurement(any()) } returns serverDto

        val result = repository.createMeasurement(130, 85, 75)
        
        // Result is returned immediately with temporary ID
        val stored = storage.values.first()
        assertEquals(SyncState.PENDING_CREATE, stored.syncState)
        assertEquals(result.id, stored.id)
    }

    @Test
    fun `syncPending uploads PENDING_CREATE and replaces with SYNCED server row`() = runBlocking {
        // 1. Create offline row
        val clientId = UUID.randomUUID().toString()
        storage[clientId] = MeasurementEntity(clientId, "2026-07-18T12:00:00Z", 130, 85, 75, SyncState.PENDING_CREATE)
        
        val serverDto = MeasurementDto("server_id_new", "2026-07-18T12:00:00Z", 130, 85, 75)
        coEvery { api.createMeasurement(any()) } returns serverDto

        // 2. Sync
        repository.syncPending()
        
        assertEquals(1, storage.size)
        val stored = storage.values.first()
        assertEquals(SyncState.SYNCED, stored.syncState)
        assertEquals("server_id_new", stored.id)
        assertNull(storage[clientId])
    }

    @Test
    fun `syncRecent does not remove PENDING_CREATE rows`() = runBlocking {
        // 1. Create offline row
        storage["client_id"] = MeasurementEntity("client_id", "2026-07-18T12:00:00Z", 130, 85, 75, SyncState.PENDING_CREATE)
        
        // 2. Refresh from server (which returns server_id_1)
        coEvery { api.getMeasurements(any(), any(), any(), any()) } returns MeasurementPageDto(
            items = listOf(MeasurementDto("server_id_1", "2026-07-18T10:00:00Z", 120, 80, 70)),
            total = 1,
            limit = 500,
            offset = 0
        )
        
        repository.syncRecent()
        
        coVerify(exactly = 1) { api.getMeasurements(any(), any(), any(), any()) }

        val all = storage.values.toList()
        assertEquals(2, all.size)
        val pending = all.find { it.syncState == SyncState.PENDING_CREATE }
        val synced = all.find { it.syncState == SyncState.SYNCED }
        
        assertEquals("client_id", pending?.id)
        assertEquals("server_id_1", synced?.id)
    }

    @Test
    fun `syncRecent hides PENDING_DELETE rows`() = runBlocking {
        // 1. Prepare synced row then fail to delete (marks pending)
        storage["server_id_old"] = MeasurementEntity("server_id_old", "2026-07-18T08:00:00Z", 120, 80, 70, SyncState.SYNCED)
        
        coEvery { api.deleteMeasurement(any()) } throws Exception("network error")
        coEvery { api.getMeasurements(any(), any(), any(), any()) } returns MeasurementPageDto(
            items = emptyList(),
            total = 0,
            limit = 500,
            offset = 0
        )
        
        repository.deleteMeasurement("server_id_old")
        
        // 2. Verify it is still in storage but hidden from getAll (handled by coEvery mockup)
        assertEquals(SyncState.PENDING_DELETE, storage["server_id_old"]?.syncState)
        assertEquals(0, repository.syncRecent().size)
        coVerify(exactly = 1) { api.getMeasurements(any(), any(), any(), any()) }
    }

    @Test
    fun `syncPending on PENDING_CREATE with 400 or 401 keeps the row`() = runBlocking {
        storage["id_400"] = MeasurementEntity("id_400", "now", 130, 85, 75, SyncState.PENDING_CREATE)
        storage["id_401"] = MeasurementEntity("id_401", "now", 140, 90, 80, SyncState.PENDING_CREATE)

        coEvery { api.createMeasurement(any()) } throws
            HttpException(Response.error<Any>(400, "".toResponseBody())) andThenThrows
            HttpException(Response.error<Any>(401, "".toResponseBody()))

        repository.syncPending()

        assertEquals(SyncState.PENDING_CREATE, storage["id_400"]?.syncState)
        assertEquals(SyncState.PENDING_CREATE, storage["id_401"]?.syncState)
    }

    @Test
    fun `syncPending on PENDING_DELETE with 404 removes row and with 401 keeps row`() = runBlocking {
        storage["del_404"] = MeasurementEntity("del_404", "now", 130, 85, 75, SyncState.PENDING_DELETE)
        storage["del_401"] = MeasurementEntity("del_401", "now", 140, 90, 80, SyncState.PENDING_DELETE)

        coEvery { api.deleteMeasurement("del_404") } throws HttpException(Response.error<Any>(404, "".toResponseBody()))
        coEvery { api.deleteMeasurement("del_401") } throws HttpException(Response.error<Any>(401, "".toResponseBody()))

        repository.syncPending()

        assertNull(storage["del_404"])
        assertEquals(SyncState.PENDING_DELETE, storage["del_401"]?.syncState)
    }

    @Test
    fun `deleteMeasurement with 401 marks the row PENDING_DELETE`() = runBlocking {
        storage["server_id"] = MeasurementEntity("server_id", "now", 130, 85, 75, SyncState.SYNCED)

        coEvery { api.deleteMeasurement("server_id") } throws HttpException(Response.error<Any>(401, "".toResponseBody()))

        repository.deleteMeasurement("server_id")

        assertEquals(SyncState.PENDING_DELETE, storage["server_id"]?.syncState)
    }
}
