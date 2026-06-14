package ua.vn.home.bptracker.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ua.vn.home.bptracker.data.dto.TodayIntake

private val dbJson = Json {
    ignoreUnknownKeys = true
}

@Entity(tableName = "med_intakes", primaryKeys = ["date", "period"])
data class MedIntakeEntity(
    val date: String, // YYYY-MM-DD
    val period: String,
    val time: String,
    val medsJson: String, // JSON array string
    val status: String?,
    val timeTaken: String?
)

fun MedIntakeEntity.toDto() = TodayIntake(
    period = period,
    time = time,
    meds = try {
        dbJson.decodeFromString<List<String>>(medsJson)
    } catch (e: Exception) {
        emptyList()
    },
    status = status,
    timeTaken = timeTaken
)

fun TodayIntake.toEntity(date: String) = MedIntakeEntity(
    date = date,
    period = period,
    time = time,
    medsJson = dbJson.encodeToString(meds),
    status = status,
    timeTaken = timeTaken
)
