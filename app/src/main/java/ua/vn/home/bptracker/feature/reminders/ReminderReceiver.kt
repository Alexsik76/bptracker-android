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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val periodFromIntent = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
        Log.i("ReminderDiag", "receiver fired action=$action extra_period=$periodFromIntent")

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = ServiceLocator.settingsStore.remindersEnabled.first()
                
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
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

                val resolved = ServiceLocator.reminderConfigRepository.resolveConfig()
                val config = resolved.config
                if (config == null) {
                    Log.e("ReminderDiag", "ReminderReceiver failure: could not resolve config")
                    ServiceLocator.settingsStore.setReminderScheduleFailed(true)
                    ServiceLocator.reminderScheduler.scheduleAlarmAt(period, LocalDateTime.now().plusHours(1))
                    Log.i("ReminderDiag", "ReminderReceiver: scheduled retry for period=$period in 1 hour")
                    return@launch
                }

                val timeStr = when (period) {
                    "Morning" -> config.morningTime
                    "Day" -> config.dayTime
                    "Evening" -> config.eveningTime
                    else -> null
                }
                if (timeStr == null) {
                    Log.e("ReminderDiag", "ReminderReceiver failure: unknown period $period")
                    return@launch
                }

                val baseTime = LocalDateTime.of(LocalDate.now(), LocalTime.parse(timeStr))
                val now = LocalDateTime.now()

                val today = LocalDate.now().toString()
                val schedule = ServiceLocator.todayScheduleUseCase.getTodayOnce(today)
                val slot = schedule.slots.find { (it.slot.name == period) }
                
                if (slot == null || slot.taken || slot.meds.isEmpty()) {
                    Log.i("ReminderDiag", "no notification for period=$period: slot=${if (slot == null) "MISSING" else if (slot.taken) "TAKEN" else "EMPTY"}. Scheduling tomorrow.")
                    ServiceLocator.reminderScheduler.scheduleAlarmAt(period, baseTime.plusDays(1))
                    return@launch
                }

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

                // Decide next alarm
                val maxReminders = config.maxReminders
                val durationMinutes = config.durationMinutes

                if (maxReminders <= 0 || durationMinutes <= 0) {
                    ServiceLocator.reminderScheduler.scheduleAlarmAt(period, baseTime.plusDays(1))
                    Log.i("ReminderDiag", "period=$period repeat skipped (config limit), scheduled for tomorrow")
                } else {
                    var nextMoment: LocalDateTime? = null
                    
                    for (k in 1..maxReminders) {
                        val candidate = baseTime.plusMinutes((durationMinutes.toLong() * k) / maxReminders)
                        if (candidate.isAfter(now.plusSeconds(5))) {
                            nextMoment = candidate
                            break
                        }
                    }

                    val durationPassed = ChronoUnit.MINUTES.between(baseTime, now)
                    val currentK = if (durationPassed <= 0) 0 else {
                        ((durationPassed.toDouble() * maxReminders) / durationMinutes).roundToInt()
                    }
                    val currentLabel = if (currentK <= 0) "primary notification" else "reminder $currentK"

                    if (nextMoment != null) {
                        ServiceLocator.reminderScheduler.scheduleAlarmAt(period, nextMoment)
                        Log.i("ReminderDiag", "period=$period fired=$currentLabel, next scheduled moment: $nextMoment")
                    } else {
                        ServiceLocator.reminderScheduler.scheduleAlarmAt(period, baseTime.plusDays(1))
                        Log.i("ReminderDiag", "period=$period fired=$currentLabel, window exhausted, scheduled for tomorrow")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
