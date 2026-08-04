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
            
            val helper = NotificationHelper(context)
            helper.cancelNotification(period)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ServiceLocator.intakeReportRepository.confirm(
                        WhenSlot.valueOf(period),
                        LocalDate.now().toString(),
                        takenAt = null
                    )

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
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
