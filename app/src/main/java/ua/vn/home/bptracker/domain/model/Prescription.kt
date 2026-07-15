package ua.vn.home.bptracker.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class Prescription(
    val id: String,
    val doctor: String,
    val prescribedOn: LocalDate,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val items: List<MedicationItem> = emptyList()
)
