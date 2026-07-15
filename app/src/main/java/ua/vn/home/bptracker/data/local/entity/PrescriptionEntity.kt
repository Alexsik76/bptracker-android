package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val doctor: String,
    val prescribedOn: String,
    val isActive: Boolean,
    val createdAt: String
)

fun PrescriptionEntity.toDto() = PrescriptionReadDto(
    id = id,
    doctor = doctor,
    prescribedOn = prescribedOn,
    isActive = isActive,
    createdAt = createdAt
)

fun PrescriptionReadDto.toEntity() = PrescriptionEntity(
    id = id,
    doctor = doctor,
    prescribedOn = prescribedOn,
    isActive = isActive,
    createdAt = createdAt
)
