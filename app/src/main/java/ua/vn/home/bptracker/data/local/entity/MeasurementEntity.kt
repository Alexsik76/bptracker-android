package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.Instant
import java.time.ZoneId

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val recordedAtMs: Long,
    val sys: Int,
    val dia: Int,
    val pulse: Int,
    val syncState: String = SyncState.SYNCED
)

fun MeasurementEntity.toDto() = MeasurementDto(
    id = id,
    recordedAt = Instant.ofEpochMilli(recordedAtMs)
        .atZone(ZoneId.systemDefault())
        .toOffsetDateTime()
        .toString(),
    sys = sys,
    dia = dia,
    pulse = pulse
)

fun MeasurementDto.toEntity(syncState: String = SyncState.SYNCED) = MeasurementEntity(
    id = id,
    recordedAtMs = TimeUtils.parseInstantOrNull(recordedAt)?.toEpochMilli()
        ?: System.currentTimeMillis(),
    sys = sys,
    dia = dia,
    pulse = pulse,
    syncState = syncState
)
