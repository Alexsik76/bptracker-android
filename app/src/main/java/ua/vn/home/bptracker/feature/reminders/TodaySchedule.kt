package ua.vn.home.bptracker.feature.reminders

import ua.vn.home.bptracker.data.dto.DoseUnit
import ua.vn.home.bptracker.data.dto.WhenSlot

data class TodaySchedule(
    val configured: Boolean,   // false when no reminder_config cached
    val date: String,          // YYYY-MM-DD
    val slots: List<TodaySlot> // only slots that have at least one med, ordered Morning→Day→Evening
)

data class TodaySlot(
    val slot: WhenSlot,
    val time: String,          // HH:MM (from config, seconds dropped)
    val meds: List<TodayMed>,
    val taken: Boolean,        // from local intake for (date, slot)
    val takenAt: String?       // the recorded moment, if taken
)

data class TodayMed(
    val medicine: String,
    val doseAmount: String,
    val doseUnit: DoseUnit?,
    val condition: String?
)
