package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object HealthConnectExportState {
    const val PENDING_UPSERT = "PENDING_UPSERT"
    const val PENDING_DELETE = "PENDING_DELETE"
    const val EXPORTED = "EXPORTED"
}

@Entity(tableName = "health_connect_export")
data class HealthConnectExportEntity(
    @PrimaryKey val measurementId: String,
    val exportState: String,
    val updatedAt: Long = System.currentTimeMillis()
)
