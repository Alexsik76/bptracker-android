package ua.vn.home.bptracker.feature.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.repository.ConfigSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

data class ReminderHealth(
    val morningAlarm: Boolean,
    val dayAlarm: Boolean,
    val eveningAlarm: Boolean,
    val exactAlarmsPermitted: Boolean,
    val notificationsPermitted: Boolean,
    val configAvailable: Boolean
) {
    val isHealthy: Boolean = morningAlarm && dayAlarm && eveningAlarm && 
            exactAlarmsPermitted && notificationsPermitted && configAvailable
}

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val invocationCounter = AtomicInteger(0)
    private val mutex = Mutex()

    suspend fun checkHealth(): ReminderHealth {
        val repository = ServiceLocator.reminderConfigRepository
        val resolved = repository.resolveConfig()
        
        val morningIntent = getAlarmIntent("Morning")
        val dayIntent = getAlarmIntent("Day")
        val eveningIntent = getAlarmIntent("Evening")

        val health = ReminderHealth(
            morningAlarm = morningIntent != null,
            dayAlarm = dayIntent != null,
            eveningAlarm = eveningIntent != null,
            exactAlarmsPermitted = canScheduleExactAlarms(),
            notificationsPermitted = areNotificationsEnabled(),
            configAvailable = resolved.source != ConfigSource.NONE
        )

        Log.i("ReminderDiag", "Health check: Morning Alarm = ${if (health.morningAlarm) "OK" else "MISSING"}")
        Log.i("ReminderDiag", "Health check: Day Alarm = ${if (health.dayAlarm) "OK" else "MISSING"}")
        Log.i("ReminderDiag", "Health check: Evening Alarm = ${if (health.eveningAlarm) "OK" else "MISSING"}")
        Log.i("ReminderDiag", "Health check: Exact Alarms Permitted = ${if (health.exactAlarmsPermitted) "OK" else "FAILED"}")
        Log.i("ReminderDiag", "Health check: Notifications Permitted = ${if (health.notificationsPermitted) "OK" else "FAILED"}")
        Log.i("ReminderDiag", "Health check: Config Available = ${if (health.configAvailable) "OK" else "FAILED"}")

        if (health.isHealthy) {
            Log.i("ReminderDiag", "Health check result: HEALTHY")
        } else {
            Log.w("ReminderDiag", "Health check result: UNHEALTHY")
        }

        return health
    }

    private fun getAlarmIntent(period: String): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            period.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun rescheduleAll() = mutex.withLock {
        val count = invocationCounter.incrementAndGet()
        val threadName = Thread.currentThread().name
        Log.i("ReminderDiag", "rescheduleAll entry [count=$count] thread=$threadName")

        if (count == 1) {
            Log.i("ReminderDiag", "startup health check (pre-repair)")
            checkHealth()
        }

        val repository = ServiceLocator.reminderConfigRepository
        val settingsStore = ServiceLocator.settingsStore

        val resolved = repository.resolveConfig()
        val config = resolved.config
        val source = resolved.source.name.lowercase()

        Log.i("ReminderDiag", "config resolved [count=$count] source=$source: ${if (config == null) "NULL" else "M=${config.morningTime}, D=${config.dayTime}, E=${config.eveningTime}"}")

        if (config == null) {
            Log.e("ReminderDiag", "rescheduleAll failure [count=$count]: no alarms will be scheduled")
            settingsStore.setReminderScheduleFailed(true)
            return@withLock
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

        scheduleAlarmAt(period, alarmTime)
    }

    fun scheduleAlarmAt(period: String, at: LocalDateTime) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_PERIOD, period)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            period.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
        Log.i("ReminderDiag", "Scheduled alarm for $period at $at (triggerAtMillis=$triggerAtMillis, exact=$isExact)")
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
