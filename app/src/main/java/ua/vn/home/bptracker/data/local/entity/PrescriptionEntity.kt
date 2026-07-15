package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto
import ua.vn.home.bptracker.domain.model.Prescription
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val doctor: String,
    val prescribedOn: String,
    val isActive: Boolean,
    val createdAt: String
)

fun PrescriptionEntity.toDomain() = Prescription(
    id = id,
    doctor = doctor,
    prescribedOn = LocalDate.parse(prescribedOn),
    isActive = isActive,
    createdAt = OffsetDateTime.parse(createdAt)
)

fun PrescriptionReadDto.toEntity() = PrescriptionEntity(
    id = id,
    doctor = doctor,
    prescribedOn = prescribedOn,
    isActive = isActive,
    createdAt = createdAt
)
