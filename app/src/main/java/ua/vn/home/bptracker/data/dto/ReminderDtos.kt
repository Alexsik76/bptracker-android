package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class TodayMeds(
    @JsonNames("Date", "date")
    val date: String,
    @JsonNames("Intakes", "intakes")
    val intakes: List<TodayIntake>
)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class TodayIntake(
    @JsonNames("Period", "period")
    val period: String,
    @SerialName("Time")
    @JsonNames("time")
    val time: String,
    @SerialName("Meds")
    @JsonNames("meds", "medications", "medicine")
    val meds: List<String> = emptyList(),
    @JsonNames("Status", "status")
    val status: String? = null,
    @JsonNames("TimeTaken", "timeTaken")
    val timeTaken: String? = null
)

@Serializable
data class ConfirmIntakeRequest(
    val period: String,
    val timezone: String
)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class ReminderTemplate(
    @JsonNames("Id", "id")
    val id: String,
    @JsonNames("SchemaId", "schemaId")
    val schemaId: String,
    @JsonNames("IsActive", "isActive")
    val isActive: Boolean,
    @JsonNames("DurationMinutes", "durationMinutes")
    val durationMinutes: Int,
    @JsonNames("MaxReminders", "maxReminders")
    val maxReminders: Int,
    @JsonNames("Periods", "periods")
    val periods: Map<String, PeriodConfig>
)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class PeriodConfig(
    @SerialName("Time")
    @JsonNames("time")
    val time: String? = null,
    @SerialName("Meds")
    @JsonNames("meds", "medications")
    val meds: List<String>? = null
)

@Serializable
data class UpdateTemplateRequest(
    val periods: Map<String, PeriodConfig>? = null,
    val durationMinutes: Int? = null,
    val maxReminders: Int? = null,
    val isActive: Boolean? = null
)
