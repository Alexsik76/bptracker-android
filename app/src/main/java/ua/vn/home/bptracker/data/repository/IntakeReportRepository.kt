package ua.vn.home.bptracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.vn.home.bptracker.data.api.IntakeReportApi
import ua.vn.home.bptracker.data.dto.IntakeReportCreateDto
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.data.local.dao.IntakeReportDao
import ua.vn.home.bptracker.data.local.entity.IntakeReportEntity
import ua.vn.home.bptracker.data.local.entity.SyncState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LocalIntake(
    val period: WhenSlot,
    val date: String,
    val takenAt: String?,
    val pendingDelete: Boolean
)

interface IntakeReportRepository {
    fun observeForDate(date: String): Flow<List<LocalIntake>>
    suspend fun refresh()
    suspend fun confirm(period: WhenSlot, date: String, takenAt: String?)
    suspend fun delete(period: WhenSlot, date: String)
}

class RealIntakeReportRepository(
    private val api: IntakeReportApi,
    private val dao: IntakeReportDao
) : IntakeReportRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeForDate(date: String): Flow<List<LocalIntake>> {
        return dao.observeForDate(date).map { entities ->
            entities.map { entity ->
                LocalIntake(
                    period = WhenSlot.valueOf(entity.period),
                    date = entity.date,
                    takenAt = if (entity.syncState == SyncState.PENDING_DELETE) null else entity.takenAt,
                    pendingDelete = entity.syncState == SyncState.PENDING_DELETE
                )
            }
        }
    }

    override suspend fun refresh() {
        try {
            val remote = api.getIntakeReports()
            val pending = dao.getPending().map { "${it.date}_${it.period}" }.toSet()
            
            remote.forEach { dto ->
                val key = "${dto.date}_${dto.period}"
                if (key !in pending) {
                    dao.upsert(
                        IntakeReportEntity(
                            date = dto.date,
                            period = dto.period.name,
                            takenAt = dto.takenAt,
                            syncState = SyncState.SYNCED,
                            serverId = dto.id,
                            recordedAt = dto.recordedAt,
                            snapshotJson = json.encodeToString(dto.snapshot)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore for offline-first
        }
    }

    override suspend fun confirm(period: WhenSlot, date: String, takenAt: String?) {
        val moment = takenAt ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        
        // Authoritative local write
        val local = IntakeReportEntity(
            date = date,
            period = period.name,
            takenAt = moment,
            syncState = SyncState.PENDING_UPSERT
        )
        dao.upsert(local)

        try {
            val result = api.createIntakeReport(
                IntakeReportCreateDto(
                    period = period,
                    date = date,
                    takenAt = moment
                )
            )
            dao.upsert(
                local.copy(
                    syncState = SyncState.SYNCED,
                    serverId = result.id,
                    recordedAt = result.recordedAt,
                    snapshotJson = json.encodeToString(result.snapshot)
                )
            )
        } catch (e: Exception) {
            // Leave as PENDING_UPSERT for worker
        }
    }

    override suspend fun delete(period: WhenSlot, date: String) {
        val existing = dao.get(date, period.name) ?: return

        if (existing.serverId == null) {
            // Never synced, just hard delete
            dao.delete(date, period.name)
        } else {
            // Mark pending and try network
            dao.markPendingDelete(date, period.name)
            try {
                val response = api.deleteIntakeReport(existing.serverId)
                if (response.isSuccessful) {
                    dao.delete(date, period.name)
                }
            } catch (e: Exception) {
                // Leave as PENDING_DELETE for worker
            }
        }
    }
}

class MockIntakeReportRepository : IntakeReportRepository {
    private val data = mutableMapOf<String, LocalIntake>() // key: date_period

    override fun observeForDate(date: String): Flow<List<LocalIntake>> {
        return kotlinx.coroutines.flow.flowOf(data.values.filter { it.date == date })
    }

    override suspend fun refresh() {}

    override suspend fun confirm(period: WhenSlot, date: String, takenAt: String?) {
        val key = "${date}_${period.name}"
        data[key] = LocalIntake(
            period = period,
            date = date,
            takenAt = takenAt ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            pendingDelete = false
        )
    }

    override suspend fun delete(period: WhenSlot, date: String) {
        val key = "${date}_${period.name}"
        data.remove(key)
    }
}
