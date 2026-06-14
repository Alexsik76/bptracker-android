package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.OffsetDateTime
import java.util.UUID

interface MeasurementRepository {
    suspend fun getMeasurements(days: Int): List<MeasurementDto>
    suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto
}

class RealMeasurementRepository(private val api: MeasurementApi) : MeasurementRepository {
    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return api.getMeasurements(days)
    }

    override suspend fun createMeasurement(sys: Int, dia: Int, pulse: Int): MeasurementDto {
        return api.createMeasurement(CreateMeasurementRequest(sys, dia, pulse))
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
}
