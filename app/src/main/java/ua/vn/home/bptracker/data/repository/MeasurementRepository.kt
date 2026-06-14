package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity
import java.time.OffsetDateTime
import java.util.UUID

interface MeasurementRepository {
    suspend fun getMeasurements(days: Int): List<MeasurementDto>
    suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto
    suspend fun deleteMeasurement(id: String)
}

class RealMeasurementRepository(
    private val api: MeasurementApi,
    private val dao: MeasurementDao
) : MeasurementRepository {
    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return try {
            val remote = api.getMeasurements(days)
            dao.deleteAll()
            dao.insertAll(remote.map { it.toEntity() })
            remote
        } catch (e: Exception) {
            dao.getAll().map { it.toDto() }
        }
    }

    override suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto {
        return try {
            val created = api.createMeasurement(CreateMeasurementRequest(sys, dia, pulse))
            dao.insert(created.toEntity())
            created
        } catch (e: Exception) {
            // If offline, we could store it with isSynced = false
            // For now, let's just throw or handle basic offline case if id is generated
            val id = UUID.randomUUID().toString()
            val local = MeasurementDto(id, OffsetDateTime.now().toString(), sys, dia, pulse)
            dao.insert(local.toEntity(synced = false))
            local
        }
    }

    override suspend fun deleteMeasurement(id: String) {
        try {
            api.deleteMeasurement(id)
            dao.deleteById(id)
        } catch (e: Exception) {
            // If it's a local unsynced one, just delete from DB
            dao.deleteById(id)
        }
    }
}

class MockMeasurementRepository : MeasurementRepository {
    private val now = OffsetDateTime.now()
    private val mockList = mutableListOf(
        MeasurementDto("1", now.minusHours(2).toString(), 118, 76, 68),   // Today morning (Optimal)
        MeasurementDto("2", now.minusHours(14).toString(), 128, 82, 72),  // Yesterday evening (Normal)
        MeasurementDto("3", now.minusHours(22).toString(), 145, 92, 75),  // Yesterday (Stage 1)
        MeasurementDto("4", now.minusDays(2).toString(), 165, 105, 80),  // Older (Stage 2)
        MeasurementDto("5", now.minusDays(3).toString(), 135, 85, 70)    // Older (Normal)
    )

    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return mockList.toList()
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
        return newReading
    }

    override suspend fun deleteMeasurement(id: String) {
        mockList.removeAll { it.id == id }
    }
}
