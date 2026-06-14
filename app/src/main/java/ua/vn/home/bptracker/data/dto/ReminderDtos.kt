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
