package ua.vn.home.bptracker.domain.model

import java.time.OffsetDateTime

data class MedicationItem(
    val id: String,
    val prescriptionId: String,
    val medicine: String,
    val condition: String?,
    val whenSlots: List<WhenSlot>,
    val doseAmount: String,
    val doseUnit: DoseUnit?,
    val freqCount: Int,
    val freqPeriod: Int,
    val freqPeriodUnit: FreqPeriodUnit,
    val courseType: CourseType,
    val courseStart: OffsetDateTime?,
    val courseIntakes: Int?
)
