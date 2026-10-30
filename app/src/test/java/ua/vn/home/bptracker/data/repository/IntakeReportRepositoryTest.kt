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
import ua.vn.home.bptracker.data.api.IntakeReportApi
import ua.vn.home.bptracker.data.dto.IntakeReportCreateDto
import ua.vn.home.bptracker.data.dto.IntakeReportReadDto
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.data.local.dao.IntakeReportDao
import ua.vn.home.bptracker.data.local.entity.IntakeReportEntity
import ua.vn.home.bptracker.data.local.entity.SyncState

class IntakeReportRepositoryTest {

    private fun httpException(code: Int): HttpException {
        return HttpException(
            Response.error<Any>(
                code,
                "".toResponseBody(null)
            )
        )
    }

    private val mockApi = object : IntakeReportApi {
        var createCalled = false
        var deleteCalled = false
        var lastDeleteId: String? = null
        var createErrorCode: Int? = null
        var deleteErrorCode: Int? = null

        override suspend fun createIntakeReport(body: IntakeReportCreateDto): IntakeReportReadDto {
            createCalled = true
            createErrorCode?.let { throw httpException(it) }
            return IntakeReportReadDto(
                id = "server_id",
                period = body.period,
                date = body.date,
                takenAt = body.takenAt ?: "now",
                recordedAt = "now",
                snapshot = emptyList()
            )
        }

        override suspend fun getIntakeReports(): List<IntakeReportReadDto> = emptyList()
        override suspend fun getIntakeReport(id: String): IntakeReportReadDto = error("stub")
        override suspend fun deleteIntakeReport(id: String): Response<Unit> {
            deleteCalled = true
            lastDeleteId = id
            deleteErrorCode?.let { throw httpException(it) }
            return Response.success(Unit)
        }
    }

    private val mockDao = object : IntakeReportDao {
        val storage = mutableMapOf<String, IntakeReportEntity>()

        override fun observeForDate(date: String): Flow<List<IntakeReportEntity>> = flowOf(storage.values.filter { it.date == date })
        override suspend fun getPending(): List<IntakeReportEntity> = storage.values.filter { it.syncState != SyncState.SYNCED }
        override suspend fun upsert(entity: IntakeReportEntity) { storage["${entity.date}_${entity.period}"] = entity }
        override suspend fun markPendingDelete(date: String, period: String) {
            storage["${date}_${period}"] = storage["${date}_${period}"]!!.copy(syncState = SyncState.PENDING_DELETE)
        }
        override suspend fun delete(date: String, period: String) { storage.remove("${date}_${period}") }
        override suspend fun deleteOld(before: String) {}
        override suspend fun deleteAll() {}
        override suspend fun get(date: String, period: String): IntakeReportEntity? = storage["${date}_${period}"]
    }

    private val repository = RealIntakeReportRepository(mockApi, mockDao)

    @Test
    fun `confirm creates PENDING_UPSERT then SYNCED on success`() = runBlocking {
        repository.confirm(WhenSlot.Morning, "2026-07-16", "2026-07-16T15:00:00")

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.SYNCED, entity?.syncState)
        assertEquals("server_id", entity?.serverId)
        assertEquals(true, mockApi.createCalled)
    }

    @Test
    fun `delete of unsynced row removes it locally without network`() = runBlocking {
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.PENDING_UPSERT,
                serverId = null
            )
        )

        repository.delete(WhenSlot.Morning, "2026-07-16")

        assertNull(mockDao.get("2026-07-16", WhenSlot.Morning.name))
        assertEquals(false, mockApi.deleteCalled)
    }

    @Test
    fun `confirm with 401 keeps the row as PENDING_UPSERT`() = runBlocking {
        mockApi.createErrorCode = 401
        repository.confirm(WhenSlot.Morning, "2026-07-16", "2026-07-16T15:00:00")

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_UPSERT, entity?.syncState)
    }

    @Test
    fun `confirm with 422 keeps the row as PENDING_UPSERT`() = runBlocking {
        mockApi.createErrorCode = 422
        repository.confirm(WhenSlot.Morning, "2026-07-16", "2026-07-16T15:00:00")

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_UPSERT, entity?.syncState)
    }

    @Test
    fun `syncPending with 4xx on a PENDING_UPSERT row keeps the row`() = runBlocking {
        mockApi.createErrorCode = 400
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.PENDING_UPSERT,
                serverId = null
            )
        )

        repository.syncPending()

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_UPSERT, entity?.syncState)
    }

    @Test
    fun `syncPending with 404 on a PENDING_DELETE row removes it`() = runBlocking {
        mockApi.deleteErrorCode = 404
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.PENDING_DELETE,
                serverId = "server_id"
            )
        )

        repository.syncPending()

        assertNull(mockDao.get("2026-07-16", WhenSlot.Morning.name))
    }

    @Test
    fun `syncPending with 401 on a PENDING_DELETE row keeps the row`() = runBlocking {
        mockApi.deleteErrorCode = 401
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.PENDING_DELETE,
                serverId = "server_id"
            )
        )

        repository.syncPending()

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_DELETE, entity?.syncState)
    }

    @Test
    fun `confirm on existing intake preserves serverId during write`() = runBlocking {
        val originalServerId = "orig-server-id-789"
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "2026-07-16T08:00:00",
                syncState = SyncState.SYNCED,
                serverId = originalServerId
            )
        )

        // When confirm is called with 401 error, serverId must not be erased
        mockApi.createErrorCode = 401
        repository.confirm(WhenSlot.Morning, "2026-07-16", "2026-07-16T10:00:00")

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_UPSERT, entity?.syncState)
        assertEquals(originalServerId, entity?.serverId)
    }

    @Test
    fun `change time of an intake then delete it sends server delete call with original serverId`() = runBlocking {
        val originalServerId = "orig-server-id-456"
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "2026-07-16T08:00:00",
                syncState = SyncState.SYNCED,
                serverId = originalServerId
            )
        )

        // Change time while offline / server returns error
        mockApi.createErrorCode = 503
        repository.confirm(WhenSlot.Morning, "2026-07-16", "2026-07-16T09:30:00")

        // Local row retains the original serverId
        val existingAfterConfirm = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(originalServerId, existingAfterConfirm?.serverId)
        assertEquals(SyncState.PENDING_UPSERT, existingAfterConfirm?.syncState)

        // Now delete it
        repository.delete(WhenSlot.Morning, "2026-07-16")

        assertEquals(true, mockApi.deleteCalled)
        assertEquals(originalServerId, mockApi.lastDeleteId)
    }

    @Test
    fun `delete with 401 keeps row as PENDING_DELETE`() = runBlocking {
        mockApi.deleteErrorCode = 401
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.SYNCED,
                serverId = "srv-1"
            )
        )

        repository.delete(WhenSlot.Morning, "2026-07-16")

        val entity = mockDao.get("2026-07-16", WhenSlot.Morning.name)
        assertEquals(SyncState.PENDING_DELETE, entity?.syncState)
    }

    @Test
    fun `delete with 404 removes row locally`() = runBlocking {
        mockApi.deleteErrorCode = 404
        mockDao.upsert(
            IntakeReportEntity(
                date = "2026-07-16",
                period = WhenSlot.Morning.name,
                takenAt = "now",
                syncState = SyncState.SYNCED,
                serverId = "srv-1"
            )
        )

        repository.delete(WhenSlot.Morning, "2026-07-16")

        assertNull(mockDao.get("2026-07-16", WhenSlot.Morning.name))
    }
}
