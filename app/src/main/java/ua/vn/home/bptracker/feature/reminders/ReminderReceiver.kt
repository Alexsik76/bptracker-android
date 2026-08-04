package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.DoseUnit
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val periodFromIntent = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
        Log.i("ReminderDiag", "receiver fired action=$action extra_period=$periodFromIntent")

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            var shouldRescheduleInFinally = true
            try {
                val enabled = ServiceLocator.settingsStore.remindersEnabled.first()
                
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    shouldRescheduleInFinally = false
                    if (enabled) {
                        Log.i("ReminderDiag", "call=ReminderReceiver.boot thread=${Thread.currentThread().name}")
                        ServiceLocator.reminderScheduler.rescheduleAll()
                    } else {
                        Log.i("ReminderDiag", "early exit ReminderReceiver.boot: reminders disabled")
                    }
                    return@launch
                }

                if (!enabled) {
                    Log.i("ReminderDiag", "early exit ReminderReceiver: reminders disabled")
                    return@launch
                }

                val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
                if (period == null) {
                    Log.i("ReminderDiag", "early exit ReminderReceiver: period extra missing")
                    return@launch
                }

                val today = LocalDate.now().toString()
                val schedule = ServiceLocator.todayScheduleUseCase.getTodayOnce(today)
                val slot = schedule.slots.find { (it.slot.name == period) }
                
                if (slot != null && (!slot.taken) && slot.meds.isNotEmpty()) {
                    val medNames = slot.meds.asSequence().map { med ->
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
                    }.toList()
                    ServiceLocator.notificationHelper.createNotificationChannel()
                    ServiceLocator.notificationHelper.showReminderNotification(period, medNames)
                    Log.i("ReminderDiag", "notification posted for period=$period")
                } else {
                    Log.i("ReminderDiag", "no notification for period=$period: slot=${if (slot == null) "MISSING" else if (slot.taken) "TAKEN" else "EMPTY"}")
                }
            } finally {
                if (shouldRescheduleInFinally) {
                    try {
                        if (ServiceLocator.settingsStore.remindersEnabled.first()) {
                            Log.i("ReminderDiag", "call=ReminderReceiver.finally thread=${Thread.currentThread().name}")
                            ServiceLocator.reminderScheduler.rescheduleAll()
                        }
                    } catch (_: Exception) {
                        // Ensure finish() is called even if rescheduling fails
                    }
                }
                pendingResult.finish()
            }
        }
    }
}
