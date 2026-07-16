package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity

object SyncState {
    const val SYNCED = "SYNCED"
    const val PENDING_UPSERT = "PENDING_UPSERT"
    const val PENDING_DELETE = "PENDING_DELETE"
}

@Entity(tableName = "intake_reports", primaryKeys = ["date", "period"])
data class IntakeReportEntity(
    val date: String, // YYYY-MM-DD
    val period: String, // WhenSlot wire value
    val takenAt: String?, // ISO datetime
    val syncState: String,
    val serverId: String? = null,
    val recordedAt: String? = null,
    val snapshotJson: String? = null // Serialized List<SnapshotEntryDto>
)
