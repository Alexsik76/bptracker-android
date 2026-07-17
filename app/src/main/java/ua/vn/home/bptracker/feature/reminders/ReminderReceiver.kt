package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.DoseUnit
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = ServiceLocator.settingsStore.remindersEnabled.first()
                if (!enabled && intent.action != Intent.ACTION_BOOT_COMPLETED) {
                    return@launch
                }

                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    if (enabled) {
                        ServiceLocator.reminderScheduler.rescheduleAll()
                    }
                    return@launch
                }

                val today = LocalDate.now().toString()
                val schedule = ServiceLocator.todayScheduleUseCase.getTodayOnce(today)
                val slot = schedule.slots.find { it.slot.name == period }
                
                if (slot != null && !slot.taken && slot.meds.isNotEmpty()) {
                    val medNames = slot.meds.map { med ->
                        val unitStr = when (med.doseUnit) {
                            DoseUnit.Tablet -> context.getString(R.string.med_enum_unit_tablet)
                            DoseUnit.Mg -> context.getString(R.string.med_enum_unit_mg)
                            DoseUnit.Ml -> context.getString(R.string.med_enum_unit_ml)
                            DoseUnit.Drop -> context.getString(R.string.med_enum_unit_drop)
                            DoseUnit.Mcg -> context.getString(R.string.med_enum_unit_mcg)
                            DoseUnit.Iu -> context.getString(R.string.med_enum_unit_iu)
                            null -> ""
                        }
                        val dose = listOf(med.doseAmount, unitStr).filter { it.isNotEmpty() }.joinToString(" ")
                        "${med.medicine} ($dose)"
                    }
                    ServiceLocator.notificationHelper.createNotificationChannel()
                    ServiceLocator.notificationHelper.showReminderNotification(period, medNames)
                }

                // Reschedule for next day
                ServiceLocator.reminderScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
