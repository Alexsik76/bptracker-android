package ua.vn.home.bptracker.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.healthconnect.HealthConnectExportWorker
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.HealthConnectExportDao
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportEntity
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportState
import ua.vn.home.bptracker.data.local.entity.SyncState
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity
import java.time.OffsetDateTime
import java.util.UUID

const val SYNC_WINDOW_DAYS = 14      // routine dashboard refresh

data class MeasurementPage(
    val items: List<MeasurementDto>,
    val total: Int
)

interface MeasurementRepository {
    suspend fun syncRecent(): List<MeasurementDto>
    suspend fun loadPage(dateFrom: OffsetDateTime?, dateTo: OffsetDateTime?, offset: Int): MeasurementPage
    /**
     * Performs a full synchronization for a specific period.
     * Fetches all remote records in pages and reconciles them with the local database.
     * Deletes local records that are absent on the server ONLY if the walk was complete.
     * @return The total number of records reported by the server.
     */
    suspend fun reconcilePeriod(dateFrom: OffsetDateTime?, dateTo: OffsetDateTime?): Int
    suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto
    suspend fun deleteMeasurement(id: String)
    suspend fun syncPending()
    suspend fun enqueueAllSyncedForExport()
    fun observeMeasurements(): Flow<List<MeasurementDto>>
}

open class RealMeasurementRepository(
    private val db: BpDatabase,
    private val api: MeasurementApi,
    private val dao: MeasurementDao,
    private val exportDao: HealthConnectExportDao? = null,
    private val settingsStore: SettingsStore? = null,
) : MeasurementRepository {
    
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private suspend fun enqueueExportForSynced(ids: List<String>) {
        if (exportDao == null || settingsStore == null || ids.isEmpty()) return
        if (settingsStore.healthConnectEnabled.first()) {
            val entities = ids.map {
                HealthConnectExportEntity(it, HealthConnectExportState.PENDING_UPSERT)
            }
            exportDao.insertIgnoreAll(entities)
            triggerWorker()
        }
    }

    private suspend fun enqueueDeleteForRecords(ids: List<String>) {
        if (exportDao == null || ids.isEmpty()) return
        for (id in ids) {
            if (exportDao.getById(id) != null) {
                exportDao.insertOrUpdate(
                    HealthConnectExportEntity(id, HealthConnectExportState.PENDING_DELETE)
                )
            }
        }
        triggerWorker()
    }

    private fun triggerWorker() {
        try {
            HealthConnectExportWorker.enqueueWork(ServiceLocator.applicationContext)
        } catch (_: Exception) {
            // Ignored if ServiceLocator context is uninitialized in unit test environment
        }
    }

    override suspend fun enqueueAllSyncedForExport() {
        if (exportDao == null) return
        val synced = dao.getAllSynced()
        if (synced.isNotEmpty()) {
            val entities = synced.map {
                HealthConnectExportEntity(it.id, HealthConnectExportState.PENDING_UPSERT)
            }
            exportDao.insertOrUpdateAll(entities)
            triggerWorker()
        }
    }

    override fun observeMeasurements(): Flow<List<MeasurementDto>> {
        return dao.getAllFlow().map { entities -> entities.map { it.toDto() } }
    }

    override suspend fun syncRecent(): List<MeasurementDto> {
        return syncMutex.withLock {
            try {
                val windowStartDt = OffsetDateTime.now().minusDays(SYNC_WINDOW_DAYS.toLong())
                val remote = api.getMeasurements(
                    dateFrom = windowStartDt.toString(),
                    limit = 500
                ).items
                
                db.withTransaction {
                    val deletedIds = dao.getAbsentSyncedIds(windowStartDt.toString(), remote.map { it.id })
                    dao.deleteAbsentSynced(windowStartDt.toString(), remote.map { it.id })
                    enqueueDeleteForRecords(deletedIds)
                    dao.insertAll(remote.map { it.toEntity(SyncState.SYNCED) })
                    enqueueExportForSynced(remote.map { it.id })
                }
                dao.getAll().map { it.toDto() }
            } catch (_: Exception) {
                dao.getAll().map { it.toDto() }
            }
        }
    }

    override suspend fun loadPage(
        dateFrom: OffsetDateTime?,
        dateTo: OffsetDateTime?,
        offset: Int
    ): MeasurementPage {
        return syncMutex.withLock {
            val remote = api.getMeasurements(
                dateFrom = dateFrom?.toString(),
                dateTo = dateTo?.toString(),
                limit = 50,
                offset = offset
            )
            
            db.withTransaction {
                dao.insertAll(remote.items.map { it.toEntity(SyncState.SYNCED) })
                enqueueExportForSynced(remote.items.map { it.id })
            }
            
            MeasurementPage(
                items = remote.items,
                total = remote.total
            )
        }
    }

    override suspend fun reconcilePeriod(dateFrom: OffsetDateTime?, dateTo: OffsetDateTime?): Int {
        return syncMutex.withLock {
            val remoteIds = mutableListOf<String>()
            var offset = 0
            var total = Int.MAX_VALUE
            var collectedCount = 0
            
            while (offset < total) {
                val page = api.getMeasurements(
                    dateFrom = dateFrom?.toString(),
                    dateTo = dateTo?.toString(),
                    limit = 50,
                    offset = offset
                )
                
                total = page.total
                remoteIds.addAll(page.items.map { it.id })
                collectedCount += page.items.size
                
                db.withTransaction {
                    dao.insertAll(page.items.map { it.toEntity(SyncState.SYNCED) })
                    enqueueExportForSynced(page.items.map { it.id })
                }
                
                if (page.items.isEmpty()) break
                offset += page.items.size
            }

            if (collectedCount != total) {
                Log.w("MeasRepo", "reconcile incomplete: collected=$collectedCount total=$total, skipping delete")
                return@withLock total
            }
            
            db.withTransaction {
                val deletedIds = dao.getAbsentSyncedIdsInRange(dateFrom?.toString(), dateTo?.toString(), remoteIds)
                dao.deleteAbsentSyncedInRange(dateFrom?.toString(), dateTo?.toString(), remoteIds)
                enqueueDeleteForRecords(deletedIds)
            }
            total
        }
    }

    override suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto {
        val id = UUID.randomUUID().toString()
        val local = MeasurementDto(id, OffsetDateTime.now().toString(), sys, dia, pulse)
        
        // 1. Optimistically save to local DB
        dao.insert(local.toEntity(SyncState.PENDING_CREATE))
        
        // 2. Launch background sync
        repositoryScope.launch {
            syncMutex.withLock {
                try {
                    val created = api.createMeasurement(CreateMeasurementRequest(sys, dia, pulse))
                    db.withTransaction {
                        dao.deleteById(id)
                        dao.insert(created.toEntity(SyncState.SYNCED))
                        enqueueExportForSynced(listOf(created.id))
                    }
                } catch (_: Exception) {
                    Log.e("MeasRepo", "Failed to sync created measurement")
                }
            }
        }
        
        // 3. Return local DTO immediately
        return local
    }

    override suspend fun deleteMeasurement(id: String) {
        val all = dao.getAll()
        val existing = all.find { it.id == id } ?: return

        if (existing.syncState == SyncState.PENDING_CREATE) {
            dao.deleteById(id)
            return
        }

        enqueueDeleteForRecords(listOf(id))

        try {
            api.deleteMeasurement(id)
            dao.deleteById(id)
        } catch (e: HttpException) {
            Log.w("MeasRepo", "Delete failed for $id: ${e.code()}")
            if (e.code() == 404) {
                dao.deleteById(id)
            } else {
                dao.markPendingDelete(id)
            }
        } catch (_: Exception) {
            dao.markPendingDelete(id)
        }
    }

    override suspend fun syncPending() {
        val pending = dao.getPending()
        pending.forEach { entity ->
            try {
                when (entity.syncState) {
                    SyncState.PENDING_CREATE -> {
                        val result = api.createMeasurement(
                            CreateMeasurementRequest(entity.sys, entity.dia, entity.pulse),
                        )
                        dao.deleteById(entity.id)
                        dao.insert(result.toEntity(SyncState.SYNCED))
                        enqueueExportForSynced(listOf(result.id))
                    }
                    SyncState.PENDING_DELETE -> {
                        enqueueDeleteForRecords(listOf(entity.id))
                        api.deleteMeasurement(entity.id)
                        dao.deleteById(entity.id)
                    }
                }
            } catch (e: HttpException) {
                Log.w("MeasRepo", "Sync failure for ${entity.id}: ${e.code()}")
                if (entity.syncState == SyncState.PENDING_DELETE && e.code() == 404) {
                    enqueueDeleteForRecords(listOf(entity.id))
                    dao.deleteById(entity.id)
                }
            } catch (_: Exception) {
                // Keep pending for next run
            }
        }
    }
}

class MockMeasurementRepository : MeasurementRepository {
    private val now = OffsetDateTime.now()
    private val mockList = mutableListOf(
        MeasurementDto("1", now.minusHours(2).toString(), 118, 76, 68),   // This morning (Optimal)
        MeasurementDto("2", now.minusHours(14).toString(), 128, 82, 72),  // Yesterday evening (Normal)
        MeasurementDto("3", now.minusHours(22).toString(), 145, 92, 75),  // Yesterday (Stage 1)
        MeasurementDto("4", now.minusDays(2).toString(), 165, 105, 80),  // Older (Stage 2)
        MeasurementDto("5", now.minusDays(3).toString(), 135, 85, 70)    // Older (Normal)
    )

    private val _stream = MutableStateFlow(mockList.toList())

    override suspend fun syncRecent(): List<MeasurementDto> {
        return _stream.value
    }

    override suspend fun loadPage(
        dateFrom: OffsetDateTime?,
        dateTo: OffsetDateTime?,
        offset: Int
    ): MeasurementPage {
        return MeasurementPage(mockList.toList(), mockList.size)
    }

    override suspend fun reconcilePeriod(dateFrom: OffsetDateTime?, dateTo: OffsetDateTime?): Int {
        // Mock reconciliation does nothing extra
        return mockList.size
    }

    override suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto {
        val newReading = MeasurementDto(
            id = UUID.randomUUID().toString(),
            recordedAt = OffsetDateTime.now().toString(),
            sys = sys,
            dia = dia,
            pulse = pulse
        )
        mockList.add(0, newReading)
        _stream.value = mockList.toList()
        return newReading
    }

    override suspend fun deleteMeasurement(id: String) {
        mockList.removeAll { it.id == id }
        _stream.value = mockList.toList()
    }

    override suspend fun syncPending() {}

    override suspend fun enqueueAllSyncedForExport() {}

    override fun observeMeasurements(): Flow<List<MeasurementDto>> = _stream.asStateFlow()
}
