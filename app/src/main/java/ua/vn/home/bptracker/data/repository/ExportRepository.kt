package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.ExportApi
import ua.vn.home.bptracker.data.dto.ExportRequest

sealed interface ExportResult {
    data object Success : ExportResult
    data object Cooldown : ExportResult
    data class Error(val message: String?) : ExportResult
}

interface ExportRepository {
    suspend fun exportCsv(timezoneId: String): ExportResult
}

class RealExportRepository(
    private val api: ExportApi
) : ExportRepository {
    override suspend fun exportCsv(timezoneId: String): ExportResult {
        return try {
            val response = api.exportCsv(ExportRequest(timezoneId))
            if (response.isSuccessful) {
                ExportResult.Success
            } else {
                if (response.code() == 429) {
                    ExportResult.Cooldown
                } else {
                    ExportResult.Error("HTTP ${response.code()}")
                }
            }
        } catch (e: Exception) {
            ExportResult.Error(e.message)
        }
    }
}

class MockExportRepository : ExportRepository {
    override suspend fun exportCsv(timezoneId: String): ExportResult {
        return ExportResult.Success
    }
}
