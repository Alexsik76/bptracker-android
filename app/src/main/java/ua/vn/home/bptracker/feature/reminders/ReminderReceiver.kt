package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.util.TimeZone

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("ReminderReceiver", "Boot completed, rescheduling all")
            rescheduleAll()
            return
        }

        val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
        Log.d("ReminderReceiver", "Alarm received for period: $period")
        
        // Fetch meds for this period and show notification
        CoroutineScope(Dispatchers.IO).launch {
            val repository = ServiceLocator.reminderRepository
            
            // 1. Show notification for current intake
            val today = repository.getToday(TimeZone.getDefault().id)
            val intake = today.intakes.find { it.period == period }
            
            if (intake != null && intake.status == null) {
                val helper = NotificationHelper(context)
                helper.showReminderNotification(period, intake.meds)
            }

            // 2. Autonomous cycle: reschedule this period for the next day
            val template = try {
                repository.getActiveTemplate()
            } catch (e: Exception) {
                null
            }
            
            if (template != null && template.isActive) {
                val config = template.periods[period]
                if (config != null) {
                    val scheduler = ReminderScheduler(context)
                    scheduler.scheduleAlarm(period, config.time)
                }
            }
        }
    }

    private fun rescheduleAll() {
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = ReminderScheduler(ServiceLocator.applicationContext)
            scheduler.rescheduleAll()
        }
    }
}
