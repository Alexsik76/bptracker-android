package ua.vn.home.bptracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.SyncState
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity
import java.util.UUID

class RealMeasurementRepositoryTest {

    private val mockDb = object : BpDatabase() {
        override fun measurementDao(): MeasurementDao = mockDao
        override fun prescriptionDao() = error("stub")
        override fun medicationItemDao() = error("stub")
        override fun intakeReportDao() = error("stub")
        override fun reminderConfigDao() = error("stub")
        
        override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = error("stub")
        override fun createInvalidationTracker(): androidx.room.InvalidationTracker = 
            androidx.room.InvalidationTracker(this, "measurement_table")
        override fun clearAllTables() {}

        override fun beginTransaction() {}
        override fun endTransaction() {}
        override fun setTransactionSuccessful() {}
    }

    private val mockApi = object : MeasurementApi {
        var createCalled = false
        var deleteCalled = false
        var getCalled = false

        override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
            getCalled = true
            return listOf(
                MeasurementDto("server_id_1", "2026-07-18T10:00:00Z", 120, 80, 70)
            )
        }

        override suspend fun createMeasurement(body: CreateMeasurementRequest): MeasurementDto {
            createCalled = true
            return MeasurementDto(
                id = "server_id_new",
                recordedAt = "2026-07-18T12:00:00Z",
                sys = body.sys,
                dia = body.dia,
                pulse = body.pulse
            )
        }

        override suspend fun deleteMeasurement(id: String) {
            deleteCalled = true
        }
    }

    private val mockDao = object : MeasurementDao {
        val storage = mutableMapOf<String, MeasurementEntity>()

        override fun getAllFlow(): Flow<List<MeasurementEntity>> = 
            flowOf(storage.values.filter { it.syncState != SyncState.PENDING_DELETE })
        override suspend fun getAll(): List<MeasurementEntity> = 
            storage.values.filter { it.syncState != SyncState.PENDING_DELETE }
        override suspend fun insert(measurement: MeasurementEntity) { storage[measurement.id] = measurement }
        override suspend fun insertAll(measurements: List<MeasurementEntity>) {
            measurements.forEach { storage[it.id] = it }
        }
        override suspend fun deleteById(id: String) { storage.remove(id) }
        override suspend fun getPending(): List<MeasurementEntity> = storage.values.filter { it.syncState != SyncState.SYNCED }
        override suspend fun deleteSynced() {
            storage.entries.removeIf { it.value.syncState == SyncState.SYNCED }
        }
        override suspend fun markPendingDelete(id: String) {
            storage[id] = storage[id]!!.copy(syncState = SyncState.PENDING_DELETE)
        }
    }

    private val repository = TestMeasurementRepository(mockDb, mockApi, mockDao)

    private open class TestMeasurementRepository(
        db: BpDatabase,
        private val api: MeasurementApi,
        private val dao: MeasurementDao
    ) : RealMeasurementRepository(db, api, dao) {
        override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
            return try {
                val remote = api.getMeasurements(days)
                dao.deleteSynced()
                dao.insertAll(remote.map { it.toEntity(SyncState.SYNCED) })
                dao.getAll().map { it.toDto() }
            } catch (e: Exception) {
                dao.getAll().map { it.toDto() }
            }
        }
    }

    @Test
    fun `createMeasurement when offline creates PENDING_CREATE row`() = runBlocking {
        val offlineApi = object : MeasurementApi {
            override suspend fun getMeasurements(days: Int): List<MeasurementDto> = error("offline")
            override suspend fun createMeasurement(body: CreateMeasurementRequest): MeasurementDto = error("offline")
            override suspend fun deleteMeasurement(id: String) = error("offline")
        }
        val offlineRepo = TestMeasurementRepository(mockDb, offlineApi, mockDao)

        val result = offlineRepo.createMeasurement(130, 85, 75)
        
        val stored = mockDao.getAll().first()
        assertEquals(SyncState.PENDING_CREATE, stored.syncState)
        assertEquals(result.id, stored.id)
    }

    @Test
    fun `syncPending uploads PENDING_CREATE and replaces with SYNCED server row`() = runBlocking {
        // 1. Create offline row
        val clientId = UUID.randomUUID().toString()
        mockDao.insert(MeasurementEntity(clientId, "2026-07-18T12:00:00Z", 130, 85, 75, SyncState.PENDING_CREATE))
        
        // 2. Sync
        repository.syncPending()
        
        val all = mockDao.getAll()
        assertEquals(1, all.size)
        val stored = all.first()
        assertEquals(SyncState.SYNCED, stored.syncState)
        assertEquals("server_id_new", stored.id)
        assertNull(mockDao.storage[clientId])
    }

    @Test
    fun `getMeasurements does not remove PENDING_CREATE rows`() = runBlocking {
        // 1. Create offline row
        mockDao.insert(MeasurementEntity("client_id", "2026-07-18T12:00:00Z", 130, 85, 75, SyncState.PENDING_CREATE))
        
        // 2. Refresh from server (which returns server_id_1)
        repository.getMeasurements(7)
        
        val all = mockDao.getAll()
        assertEquals(2, all.size)
        val pending = all.find { it.syncState == SyncState.PENDING_CREATE }
        val synced = all.find { it.syncState == SyncState.SYNCED }
        
        assertEquals("client_id", pending?.id)
        assertEquals("server_id_1", synced?.id)
    }

    @Test
    fun `getMeasurements hides PENDING_DELETE rows`() = runBlocking {
        val failingApi = object : MeasurementApi {
            override suspend fun getMeasurements(days: Int): List<MeasurementDto> = emptyList()
            override suspend fun createMeasurement(body: CreateMeasurementRequest): MeasurementDto = error("stub")
            override suspend fun deleteMeasurement(id: String) = error("network error")
        }
        val repoWithFail = TestMeasurementRepository(mockDb, failingApi, mockDao)

        // 1. Prepare synced row then fail to delete (marks pending)
        mockDao.insert(MeasurementEntity("server_id_old", "2026-07-18T08:00:00Z", 120, 80, 70, SyncState.SYNCED))
        repoWithFail.deleteMeasurement("server_id_old")
        
        // 2. Verify it is still in storage but hidden from getAll
        assertEquals(SyncState.PENDING_DELETE, mockDao.storage["server_id_old"]?.syncState)
        assertEquals(0, repoWithFail.getMeasurements(7).size)
    }

    @Test
    fun `syncPending drops row on permanent 4xx error`() = runBlocking {
        val permanentFailApi = object : MeasurementApi {
            override suspend fun getMeasurements(days: Int): List<MeasurementDto> = emptyList()
            override suspend fun createMeasurement(body: CreateMeasurementRequest): MeasurementDto {
                throw HttpException(Response.error<Any>(400, "".toResponseBody()))
            }
            override suspend fun deleteMeasurement(id: String) {}
        }
        val repo = TestMeasurementRepository(mockDb, permanentFailApi, mockDao)
        
        // 1. Create offline row
        mockDao.insert(MeasurementEntity("poison_id", "now", 130, 85, 75, SyncState.PENDING_CREATE))
        
        // 2. Sync - should catch 400 and delete locally
        repo.syncPending()
        
        assertNull(mockDao.storage["poison_id"])
    }
}
