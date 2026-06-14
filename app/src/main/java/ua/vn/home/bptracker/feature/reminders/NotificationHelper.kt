package ua.vn.home.bptracker.feature.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ua.vn.home.bptracker.MainActivity
import ua.vn.home.bptracker.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "med_reminders"
        const val ACTION_TAKEN = "ua.vn.home.bptracker.ACTION_TAKEN"
        const val EXTRA_PERIOD = "extra_period"
        const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medication Reminders"
            val descriptionText = "Notifications for scheduled medications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(period: String, meds: List<String>) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_PERIOD, period)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, 
            period.hashCode(), 
            takenIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val medList = meds.joinToString(", ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use medication icon
            .setContentTitle("Time to take your meds: $period")
            .setContentText(medList)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Taken", takenPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + period.hashCode(), notification)
    }
    
    fun cancelNotification(period: String) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + period.hashCode())
    }
}
