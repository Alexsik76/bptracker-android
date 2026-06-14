package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.OffsetDateTime

interface MeasurementRepository {
    suspend fun getMeasurements(days: Int): List<MeasurementDto>
}

class RealMeasurementRepository(private val api: MeasurementApi) : MeasurementRepository {
    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        return api.getMeasurements(days)
    }
}

class MockMeasurementRepository : MeasurementRepository {
    override suspend fun getMeasurements(days: Int): List<MeasurementDto> {
        val now = OffsetDateTime.now()
        // Mocking: 1 today morning, 2 yesterday
        return listOf(
            MeasurementDto("1", now.minusHours(2).toString(), 118, 76, 68),   // Today morning (Optimal)
            MeasurementDto("2", now.minusHours(14).toString(), 128, 82, 72),  // Yesterday evening (Normal)
            MeasurementDto("3", now.minusHours(22).toString(), 145, 92, 75),  // Yesterday (Stage 1)
            MeasurementDto("4", now.minusDays(2).toString(), 165, 105, 80),  // Older (Stage 2)
            MeasurementDto("5", now.minusDays(3).toString(), 135, 85, 70)    // Older (Normal)
        )
    }
}
