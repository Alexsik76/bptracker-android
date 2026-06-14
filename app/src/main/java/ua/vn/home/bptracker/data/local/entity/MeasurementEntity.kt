package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.data.dto.MeasurementDto

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val recordedAt: String,
    val sys: Int,
    val dia: Int,
    val pulse: Int,
    val isSynced: Boolean = true
)

fun MeasurementEntity.toDto() = MeasurementDto(
    id = id,
    recordedAt = recordedAt,
    sys = sys,
    dia = dia,
    pulse = pulse
)

fun MeasurementDto.toEntity(synced: Boolean = true) = MeasurementEntity(
    id = id,
    recordedAt = recordedAt,
    sys = sys,
    dia = dia,
    pulse = pulse,
    isSynced = synced
)
