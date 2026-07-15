package ua.vn.home.bptracker.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() {
        val repository = ServiceLocator.reminderRepository
        val template = try {
            repository.getActiveTemplate()
        } catch (e: Exception) {
            null
        }

        if (template == null || !template.isActive) {
            cancelAllReminders()
            return
        }

        template.periods.forEach { (period, config) ->
            if (config.time != null) {
                scheduleAlarm(period, config.time)
            }
        }
    }

    fun cancelAllReminders() {
        // Cancel for canonical periods
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

        // If time already passed today, schedule for tomorrow
        if (alarmTime.isBefore(now)) {
            alarmTime = alarmTime.plusDays(1)
        } else {
            // If it's a new time for TODAY, we should clear the local 'Missed' status
            // so that getToday() fetches/shows the updated status from server or becomes null (pending)
            val database = ua.vn.home.bptracker.data.local.BpDatabase.build(context)
            CoroutineScope(Dispatchers.IO).launch {
                database.medIntakeDao().deleteByDateAndPeriod(LocalDate.now().toString(), period)
            }
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

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            // Fallback to inexact but working while idle
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        Log.d("ReminderScheduler", "Scheduled alarm for $period at $alarmTime")
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
