package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.vn.home.bptracker.data.dto.MedicationItemReadDto
import ua.vn.home.bptracker.domain.model.*
import java.time.OffsetDateTime

private val dbJson = Json {
    ignoreUnknownKeys = true
}

@Entity(
    tableName = "medication_items",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("prescriptionId")]
)
data class MedicationItemEntity(
    @PrimaryKey val id: String,
    val prescriptionId: String,
    val medicine: String,
    val condition: String?,
    val whenSlotsJson: String,
    val doseAmount: String,
    val doseUnit: String?,
    val freqCount: Int,
    val freqPeriod: Int,
    val freqPeriodUnit: String,
    val courseType: String,
    val courseStart: String?,
    val courseIntakes: Int?
)

fun MedicationItemEntity.toDomain() = MedicationItem(
    id = id,
    prescriptionId = prescriptionId,
    medicine = medicine,
    condition = condition,
    whenSlots = try {
        dbJson.decodeFromString<List<WhenSlot>>(whenSlotsJson)
    } catch (e: Exception) {
        emptyList()
    },
    doseAmount = doseAmount,
    doseUnit = doseUnit?.let { enumValueOf<DoseUnit>(it) },
    freqCount = freqCount,
    freqPeriod = freqPeriod,
    freqPeriodUnit = enumValueOf<FreqPeriodUnit>(freqPeriodUnit),
    courseType = enumValueOf<CourseType>(courseType),
    courseStart = courseStart?.let { OffsetDateTime.parse(it) },
    courseIntakes = courseIntakes
)

fun MedicationItemReadDto.toEntity() = MedicationItemEntity(
    id = id,
    prescriptionId = prescriptionId,
    medicine = medicine,
    condition = condition,
    whenSlotsJson = dbJson.encodeToString(whenSlots),
    doseAmount = doseAmount,
    doseUnit = doseUnit?.name,
    freqCount = freqCount,
    freqPeriod = freqPeriod,
    freqPeriodUnit = freqPeriodUnit.name,
    courseType = courseType.name,
    courseStart = courseStart,
    courseIntakes = courseIntakes
)
