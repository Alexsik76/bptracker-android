package ua.vn.home.bptracker.feature.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ua.vn.home.bptracker.MainActivity
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.data.dto.WhenSlot
import java.time.LocalDate

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "med_reminders"
        const val ACTION_TAKEN = "ua.vn.home.bptracker.ACTION_TAKEN"
        const val EXTRA_PERIOD = "extra_period"
        const val EXTRA_DATE = "extra_date"

        private fun slotIndex(slot: WhenSlot): Int = when (slot) {
            WhenSlot.Morning -> 0
            WhenSlot.Day -> 1
            WhenSlot.Evening -> 2
        }

        fun eventKey(date: LocalDate, slot: WhenSlot): Int =
            (date.toEpochDay() * 3 + slotIndex(slot)).toInt()
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val name = "Medication Reminders"
        val descriptionText = "Notifications for scheduled medications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showReminderNotification(date: LocalDate, slot: WhenSlot, meds: List<String>) {
        val key = eventKey(date, slot)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            key * 4 + 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_PERIOD, slot.name)
            putExtra(EXTRA_DATE, date.toString())
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            key * 4 + 2,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val medList = meds.joinToString(", ")
        val title = context.getString(R.string.notification_reminder_title, slot.name)
        val actionText = context.getString(R.string.notification_action_taken)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(medList)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, actionText, takenPendingIntent)
            .build()

        notificationManager.notify(key * 4 + 0, notification)
    }

    fun cancelNotification(date: LocalDate, slot: WhenSlot) {
        val key = eventKey(date, slot)
        notificationManager.cancel(key * 4 + 0)
    }
}
