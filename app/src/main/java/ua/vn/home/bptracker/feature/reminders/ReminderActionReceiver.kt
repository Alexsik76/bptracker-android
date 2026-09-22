package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_TAKEN) {
            val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
            val dateStr = intent.getStringExtra(NotificationHelper.EXTRA_DATE)

            val date = if (dateStr != null) {
                try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    Log.w("ReminderDiag", "Failed to parse EXTRA_DATE: $dateStr, falling back to LocalDate.now()")
                    LocalDate.now()
                }
            } else {
                Log.w("ReminderDiag", "EXTRA_DATE missing, falling back to LocalDate.now()")
                LocalDate.now()
            }

            val slot = WhenSlot.valueOf(period)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ServiceLocator.confirmIntakeUseCase(slot, date, takenAt = null)

                    if (date == LocalDate.now()) {
                        val resolved = ServiceLocator.reminderConfigRepository.resolveConfig()
                        val config = resolved.config
                        if (config != null) {
                            val timeStr = when (period) {
                                "Morning" -> config.morningTime
                                "Day" -> config.dayTime
                                "Evening" -> config.eveningTime
                                else -> null
                            }
                            if (timeStr != null) {
                                val baseTime = LocalDateTime.of(LocalDate.now(), LocalTime.parse(timeStr))
                                ServiceLocator.reminderScheduler.scheduleAlarmAt(period, baseTime.plusDays(1))
                            }
                        } else {
                            Log.e("ReminderDiag", "ReminderActionReceiver failure: could not resolve config for reschedule")
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
