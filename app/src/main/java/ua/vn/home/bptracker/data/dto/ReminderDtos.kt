package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TodayMeds(
    val date: String,
    val intakes: List<TodayIntake>
)

@Serializable
data class TodayIntake(
    val period: String,
    val time: String,
    val meds: List<String>,
    val status: String?, // "Confirmed", "Missed", or null (pending)
    val timeTaken: String?
)

@Serializable
data class ConfirmIntakeRequest(
    val period: String,
    val timezone: String
)

@Serializable
data class ReminderTemplate(
    val id: String,
    val schemaId: String,
    val isActive: Boolean,
    val durationMinutes: Int,
    val maxReminders: Int,
    val periods: Map<String, PeriodConfig>
)

@Serializable
data class PeriodConfig(
    val time: String = "08:00",
    val meds: List<String> = emptyList()
)

@Serializable
data class UpdateTemplateRequest(
    val periods: Map<String, PeriodConfig>? = null,
    val durationMinutes: Int? = null,
    val maxReminders: Int? = null,
    val isActive: Boolean? = null
)
