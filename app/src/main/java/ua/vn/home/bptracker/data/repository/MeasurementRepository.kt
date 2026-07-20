package ua.vn.home.bptracker.data.repository

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.SyncState
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity
import java.time.OffsetDateTime
import java.util.UUID

interface MeasurementRepository {
    suspend fun getMeasurements(days: Int): List<MeasurementDto>
    suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto
    suspend fun deleteMeasurement(id: String)
    suspend fun syncPending()
    fun observeMeasurements(): Flow<List<MeasurementDto>>
}

open class RealMeasurementRepository(
    private val db: BpDatabase,
    private val api: MeasurementApi,
    private val dao: MeasurementDao
) : MeasurementRepository {
    
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeMeasurements(): Flow<List<MeasurementDto>> {
        return dao.getAllFlow().map { entities -> entities.map { it.toDto() } }
    }
    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return try {
            val remote = api.getMeasurements(days)
            db.withTransaction {
                dao.deleteSynced()
                dao.insertAll(remote.map { it.toEntity(SyncState.SYNCED) })
            }
            dao.getAll().map { it.toDto() }
        } catch (e: Exception) {
            dao.getAll().map { it.toDto() }
        }
    }

    override suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto {
        val id = UUID.randomUUID().toString()
        val local = MeasurementDto(id, OffsetDateTime.now().toString(), sys, dia, pulse)
        
        // 1. Optimistically save to local DB
        dao.insert(local.toEntity(SyncState.PENDING_CREATE))
        
        // 2. Launch background sync
        repositoryScope.launch {
            try {
                val created = api.createMeasurement(CreateMeasurementRequest(sys, dia, pulse))
                db.withTransaction {
                    dao.deleteById(id)
                    dao.insert(created.toEntity(SyncState.SYNCED))
                }
            } catch (e: Exception) {
                Log.e("MeasRepo", "Failed to sync created measurement: ${e.message}")
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

        try {
            api.deleteMeasurement(id)
            dao.deleteById(id)
        } catch (e: HttpException) {
            if (e.code() in 400..499) {
                // 404 or other 4xx means we should just drop it locally
                dao.deleteById(id)
            } else {
                dao.markPendingDelete(id)
            }
        } catch (e: Exception) {
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
                            CreateMeasurementRequest(entity.sys, entity.dia, entity.pulse)
                        )
                        dao.deleteById(entity.id)
                        dao.insert(result.toEntity(SyncState.SYNCED))
                    }
                    SyncState.PENDING_DELETE -> {
                        api.deleteMeasurement(entity.id)
                        dao.deleteById(entity.id)
                    }
                }
            } catch (e: HttpException) {
                if (e.code() in 400..499) {
                    Log.w("MeasRepo", "Permanent sync failure for ${entity.id}: ${e.code()}")
                    dao.deleteById(entity.id)
                }
            } catch (e: Exception) {
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

    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return _stream.value
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

    override fun observeMeasurements(): Flow<List<MeasurementDto>> = _stream.asStateFlow()
}
