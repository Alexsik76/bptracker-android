package ua.vn.home.bptracker.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val invocationCounter = AtomicInteger(0)

    suspend fun rescheduleAll() {
        val count = invocationCounter.incrementAndGet()
        val threadName = Thread.currentThread().name
        Log.i("ReminderDiag", "rescheduleAll entry [count=$count] thread=$threadName")

        val repository = ServiceLocator.reminderConfigRepository
        val settingsStore = ServiceLocator.settingsStore

        val resolved = repository.resolveConfig()
        val config = resolved.config
        val source = resolved.source.name.lowercase()

        Log.i("ReminderDiag", "config resolved [count=$count] source=$source: ${if (config == null) "NULL" else "M=${config.morningTime}, D=${config.dayTime}, E=${config.eveningTime}"}")

        if (config == null) {
            Log.e("ReminderDiag", "rescheduleAll failure [count=$count]: no alarms will be scheduled")
            settingsStore.setReminderScheduleFailed(true)
            return
        }

        settingsStore.setReminderScheduleFailed(false)

        Log.i("ReminderDiag", "rescheduleAll before cancelAll [count=$count]")
        cancelAllReminders()
        Log.i("ReminderDiag", "rescheduleAll after cancelAll [count=$count]")

        scheduleAlarm("Morning", config.morningTime)
        scheduleAlarm("Day", config.dayTime)
        scheduleAlarm("Evening", config.eveningTime)

        Log.i("ReminderDiag", "rescheduleAll exit [count=$count]")
    }

    fun cancelAllReminders() {
        listOf("Morning", "Day", "Evening").forEach { period ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                period.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun scheduleAlarm(period: String, timeStr: String) {
        val time = try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Invalid time format: $timeStr", e)
            return
        }

        val now = LocalDateTime.now()
        var alarmTime = LocalDateTime.of(LocalDate.now(), time)

        if (alarmTime.isBefore(now)) {
            alarmTime = alarmTime.plusDays(1)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_PERIOD, period)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            period.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val isExact = canScheduleExactAlarms()
        if (isExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            Log.w("ReminderDiag", "Exact alarms not permitted, falling back to inexact scheduling for $period")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        Log.i("ReminderDiag", "Scheduled alarm for $period at $alarmTime (triggerAtMillis=$triggerAtMillis, exact=$isExact)")
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
