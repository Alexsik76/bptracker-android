package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDate

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_TAKEN) {
            val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) ?: return
            
            val helper = NotificationHelper(context)
            helper.cancelNotification(period)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ServiceLocator.intakeReportRepository.confirm(
                        WhenSlot.valueOf(period),
                        LocalDate.now().toString(),
                        takenAt = null
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
