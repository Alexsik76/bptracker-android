package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.vn.home.bptracker.data.dto.*

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
    val doseUnitJson: String?,
    val freqCount: Int,
    val freqPeriod: Int,
    val freqPeriodUnitJson: String,
    val courseTypeJson: String,
    val courseStart: String?,
    val courseIntakes: Int?
)

fun MedicationItemEntity.toDto() = MedicationItemReadDto(
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
    doseUnit = doseUnitJson?.let { 
        try { dbJson.decodeFromString<DoseUnit>(it) } catch (e: Exception) { null }
    },
    freqCount = freqCount,
    freqPeriod = freqPeriod,
    freqPeriodUnit = try {
        dbJson.decodeFromString<FreqPeriodUnit>(freqPeriodUnitJson)
    } catch (e: Exception) {
        FreqPeriodUnit.Day // Fallback
    },
    courseType = try {
        dbJson.decodeFromString<CourseType>(courseTypeJson)
    } catch (e: Exception) {
        CourseType.Ongoing
    },
    courseStart = courseStart,
    courseIntakes = courseIntakes
)

fun MedicationItemReadDto.toEntity() = MedicationItemEntity(
    id = id,
    prescriptionId = prescriptionId,
    medicine = medicine,
    condition = condition,
    whenSlotsJson = dbJson.encodeToString(whenSlots),
    doseAmount = doseAmount,
    doseUnitJson = doseUnit?.let { dbJson.encodeToString(it) },
    freqCount = freqCount,
    freqPeriod = freqPeriod,
    freqPeriodUnitJson = dbJson.encodeToString(freqPeriodUnit),
    courseTypeJson = dbJson.encodeToString(courseType),
    courseStart = courseStart,
    courseIntakes = courseIntakes
)
