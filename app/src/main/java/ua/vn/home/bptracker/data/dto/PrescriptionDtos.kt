package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable
import ua.vn.home.bptracker.domain.model.*

@Serializable
data class PrescriptionReadDto(
    val id: String,
    val doctor: String,
    val prescribedOn: String, // YYYY-MM-DD
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class PrescriptionCreateDto(
    val doctor: String,
    val prescribedOn: String,
    val isActive: Boolean = true
)

@Serializable
data class PrescriptionPatchDto(
    val doctor: String? = null,
    val prescribedOn: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class MedicationItemReadDto(
    val id: String,
    val prescriptionId: String,
    val medicine: String,
    val condition: String? = null,
    val whenSlots: List<WhenSlot>,
    val doseAmount: String,
    val doseUnit: DoseUnit? = null,
    val freqCount: Int,
    val freqPeriod: Int,
    val freqPeriodUnit: FreqPeriodUnit,
    val courseType: CourseType,
    val courseStart: String? = null,
    val courseIntakes: Int? = null
)

@Serializable
data class MedicationItemCreateDto(
    val medicine: String,
    val condition: String? = null,
    val whenSlots: List<WhenSlot>,
    val doseAmount: String,
    val doseUnit: DoseUnit? = null,
    val freqCount: Int,
    val freqPeriod: Int,
    val freqPeriodUnit: FreqPeriodUnit,
    val courseType: CourseType = CourseType.Ongoing,
    val courseStart: String? = null,
    val courseIntakes: Int? = null
)

@Serializable
data class MedicationItemPatchDto(
    val medicine: String? = null,
    val condition: String? = null,
    val whenSlots: List<WhenSlot>? = null,
    val doseAmount: String? = null,
    val doseUnit: DoseUnit? = null,
    val freqCount: Int? = null,
    val freqPeriod: Int? = null,
    val freqPeriodUnit: FreqPeriodUnit? = null,
    val courseType: CourseType? = null,
    val courseStart: String? = null,
    val courseIntakes: Int? = null
)
