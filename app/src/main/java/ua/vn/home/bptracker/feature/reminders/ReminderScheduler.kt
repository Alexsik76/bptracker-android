package ua.vn.home.bptracker.feature.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleTodayReminders() {
        if (!canScheduleExactAlarms()) return

        val repository = ServiceLocator.reminderRepository
        val today = repository.getToday(TimeZone.getDefault().id)
        
        today.intakes.forEach { intake ->
            if (intake.status == null) {
                scheduleAlarm(intake.period, intake.time)
            }
        }
    }

    private fun scheduleAlarm(period: String, timeStr: String) {
        val time = LocalTime.parse(timeStr)
        val now = LocalDateTime.now()
        var alarmTime = LocalDateTime.of(LocalDate.now(), time)

        if (alarmTime.isBefore(now)) {
            // Already passed today, don't schedule or schedule for tomorrow if we want continuous
            // Plan says "Today", so we only care about future ones today for now.
            return
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
