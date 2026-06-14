package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.util.TimeZone

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all on reboot
            rescheduleAll()
            return
        }

        val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
        
        // Fetch meds for this period and show notification
        CoroutineScope(Dispatchers.IO).launch {
            val repository = ServiceLocator.reminderRepository
            val today = repository.getToday(TimeZone.getDefault().id)
            val intake = today.intakes.find { it.period == period }
            
            if (intake != null && intake.status == null) {
                val helper = NotificationHelper(context)
                helper.showReminderNotification(period, intake.meds)
            }
        }
    }

    private fun rescheduleAll() {
        CoroutineScope(Dispatchers.IO).launch {
            val scheduler = ReminderScheduler(ServiceLocator.applicationContext)
            scheduler.scheduleTodayReminders()
        }
    }
}
