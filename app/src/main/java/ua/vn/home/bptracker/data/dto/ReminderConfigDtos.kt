package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReminderConfigDto(
    val morningTime: String, // HH:MM:SS
    val dayTime: String,
    val eveningTime: String,
    val maxReminders: Int,
    val durationMinutes: Int
)
