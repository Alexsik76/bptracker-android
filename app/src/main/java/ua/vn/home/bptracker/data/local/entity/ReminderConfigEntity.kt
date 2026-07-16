package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.data.dto.ReminderConfigDto

@Entity(tableName = "reminder_config")
data class ReminderConfigEntity(
    @PrimaryKey val id: Int = 0,
    val morningTime: String,
    val dayTime: String,
    val eveningTime: String,
    val maxReminders: Int,
    val durationMinutes: Int
)

fun ReminderConfigEntity.toDto() = ReminderConfigDto(
    morningTime = morningTime,
    dayTime = dayTime,
    eveningTime = eveningTime,
    maxReminders = maxReminders,
    durationMinutes = durationMinutes
)

fun ReminderConfigDto.toEntity() = ReminderConfigEntity(
    morningTime = morningTime,
    dayTime = dayTime,
    eveningTime = eveningTime,
    maxReminders = maxReminders,
    durationMinutes = durationMinutes
)
