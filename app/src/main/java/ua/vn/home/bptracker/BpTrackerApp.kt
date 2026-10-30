package ua.vn.home.bptracker

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.feature.reminders.NotificationHelper
import ua.vn.home.bptracker.feature.reminders.ReminderScheduler

class BpTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Initialize notification channels
        NotificationHelper(this).createNotificationChannel()

        // Schedule reminders
        CoroutineScope(Dispatchers.Default).launch {
            if (ServiceLocator.settingsStore.remindersEnabled.first()) {
                Log.i("ReminderDiag", "call=BpTrackerApp.onCreate thread=${Thread.currentThread().name}")
                ServiceLocator.reminderScheduler.rescheduleAll()
            }
        }
    }
}
