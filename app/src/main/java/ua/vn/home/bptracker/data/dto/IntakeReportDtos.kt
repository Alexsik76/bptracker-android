package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class IntakeReportCreateDto(
    val period: WhenSlot,
    val date: String, // YYYY-MM-DD
    val takenAt: String? = null // ISO datetime
)

@Serializable
data class SnapshotEntryDto(
    val medicine: String,
    val amount: String,
    val condition: String? = null
)

@Serializable
data class IntakeReportReadDto(
    val id: String,
    val period: WhenSlot,
    val date: String,
    val takenAt: String,
    val recordedAt: String,
    val snapshot: List<SnapshotEntryDto>
)
