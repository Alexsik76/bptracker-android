package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ua.vn.home.bptracker.data.dto.TodayIntake

@Entity(tableName = "med_intakes")
data class MedIntakeEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val date: String, // YYYY-MM-DD
    val period: String,
    val time: String,
    val meds: String, // Comma separated
    val status: String?,
    val timeTaken: String?
)

fun MedIntakeEntity.toDto() = TodayIntake(
    period = period,
    time = time,
    meds = meds.split(",").filter { it.isNotBlank() },
    status = status,
    timeTaken = timeTaken
)

fun TodayIntake.toEntity(date: String) = MedIntakeEntity(
    date = date,
    period = period,
    time = time,
    meds = meds.joinToString(","),
    status = status,
    timeTaken = timeTaken
)
