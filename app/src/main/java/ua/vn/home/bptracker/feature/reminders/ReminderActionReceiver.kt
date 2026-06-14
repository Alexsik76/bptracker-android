package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import java.util.TimeZone

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_TAKEN) {
            val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
            
            val helper = NotificationHelper(context)
            helper.cancelNotification(period)

            CoroutineScope(Dispatchers.IO).launch {
                ServiceLocator.reminderRepository.confirm(period, TimeZone.getDefault().id)
            }
        }
    }
}
