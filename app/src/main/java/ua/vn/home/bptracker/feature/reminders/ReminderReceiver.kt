package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.DoseUnit
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.data.repository.ReminderConfigRepository
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderReceiver(
    private val schedulerOverride: ReminderScheduler? = null,
    private val configRepoOverride: ReminderConfigRepository? = null,
    private val todayScheduleOverride: TodayScheduleUseCase? = null,
    private val settingsStoreOverride: SettingsStore? = null,
    private val notificationHelperOverride: NotificationHelper? = null,
    private val coroutineScopeOverride: CoroutineScope? = null
) : BroadcastReceiver() {

    private fun getScheduler(): ReminderScheduler = schedulerOverride ?: ServiceLocator.reminderScheduler
    private fun getConfigRepository(): ReminderConfigRepository = configRepoOverride ?: ServiceLocator.reminderConfigRepository
    private fun getTodayScheduleUseCase(): TodayScheduleUseCase = todayScheduleOverride ?: ServiceLocator.todayScheduleUseCase
    private fun getSettingsStore(): SettingsStore = settingsStoreOverride ?: ServiceLocator.settingsStore
    private fun getNotificationHelper(context: Context): NotificationHelper =
        notificationHelperOverride ?: NotificationHelper(context)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val periodFromIntent = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
        Log.i("ReminderDiag", "receiver fired action=$action extra_period=$periodFromIntent")

        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        val scope = coroutineScopeOverride ?: CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                processIntent(context, intent)
            } finally {
                try {
                    pendingResult?.finish()
                } catch (_: Exception) {
                }
            }
        }
    }

    internal suspend fun processIntent(context: Context, intent: Intent) {
        val action = intent.action

        try {
            val enabled = getSettingsStore().remindersEnabled.first()

            val isRestoreAction = action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == Intent.ACTION_TIMEZONE_CHANGED ||
                    action == Intent.ACTION_TIME_CHANGED

            if (isRestoreAction) {
                if (enabled) {
                    Log.i("ReminderDiag", "call=ReminderReceiver.restore action=$action thread=${Thread.currentThread().name}")
                    getScheduler().rescheduleAll()
                } else {
                    Log.i("ReminderDiag", "early exit ReminderReceiver.restore: reminders disabled for action=$action")
                }
                return
            }

            if (!enabled) {
                Log.i("ReminderDiag", "early exit ReminderReceiver: reminders disabled")
                return
            }

            val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
            if (period == null) {
                Log.i("ReminderDiag", "early exit ReminderReceiver: period extra missing")
                return
            }

            val resolved = getConfigRepository().resolveConfig()
            val config = resolved.config ?: getConfigRepository().getCachedConfig() ?: DEFAULT_REMINDER_CONFIG

            val slot = WhenSlot.valueOf(period)
            val now = LocalDateTime.now()

            val today = now.toLocalDate().toString()
            val schedule = getTodayScheduleUseCase().getTodayOnce(today)
            val todaySlot = schedule.slots.find { it.slot == slot }

            if (todaySlot == null || todaySlot.taken || todaySlot.meds.isEmpty()) {
                Log.i("ReminderDiag", "no notification for period=$period: slot=${if (todaySlot == null) "MISSING" else if (todaySlot.taken) "TAKEN" else "EMPTY"}. Scheduling tomorrow.")
                val nextMoment = computeNextReminder(slot, config, isConfirmed = true, now = now)
                getScheduler().scheduleAlarmAt(period, nextMoment)
                return
            }

            val medNames = todaySlot.meds.asSequence().map { med ->
                val unitStr = when (med.doseUnit) {
                    DoseUnit.Tablet -> context.getString(R.string.med_enum_unit_tablet)
                    DoseUnit.Mg -> context.getString(R.string.med_enum_unit_mg)
                    DoseUnit.Ml -> context.getString(R.string.med_enum_unit_ml)
                    DoseUnit.Drop -> context.getString(R.string.med_enum_unit_drop)
                    DoseUnit.Mcg -> context.getString(R.string.med_enum_unit_mcg)
                    DoseUnit.Iu -> context.getString(R.string.med_enum_unit_iu)
                    null -> ""
                }
                val dose = listOf(med.doseAmount, unitStr).filter { it.isNotEmpty() }.joinToString(" ")
                "${med.medicine} ($dose)"
            }.toList()

            getNotificationHelper(context).createNotificationChannel()
            getNotificationHelper(context).showReminderNotification(now.toLocalDate(), slot, medNames)
            Log.i("ReminderDiag", "notification posted for period=$period")

            val nextMoment = computeNextReminder(slot, config, isConfirmed = false, now = now)
            getScheduler().scheduleAlarmAt(period, nextMoment)
            Log.i("ReminderDiag", "period=$period next scheduled moment: $nextMoment")
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e("ReminderDiag", "ReminderReceiver failure in processIntent", t)
            try {
                scheduleFallback(intent)
            } catch (fallbackError: Throwable) {
                Log.e("ReminderDiag", "Failed to schedule fallback alarm", fallbackError)
            }
        }
    }

    private suspend fun scheduleFallback(intent: Intent) {
        val period = intent.getStringExtra(NotificationHelper.EXTRA_PERIOD)
        val fallbackConfig = try {
            getConfigRepository().getCachedConfig()
        } catch (_: Throwable) {
            null
        } ?: DEFAULT_REMINDER_CONFIG
        val now = LocalDateTime.now()

        if (period != null) {
            val slot = WhenSlot.entries.find { it.name.equals(period, ignoreCase = true) }
            val nextMoment = if (slot != null) {
                computeNextReminder(slot, fallbackConfig, isConfirmed = false, now = now)
            } else {
                now.plusHours(1)
            }
            getScheduler().scheduleAlarmAt(period, nextMoment)
            Log.i("ReminderDiag", "Scheduled fallback alarm for period=$period at $nextMoment")
        } else {
            WhenSlot.entries.forEach { slot ->
                val nextMoment = computeNextReminder(slot, fallbackConfig, isConfirmed = false, now = now)
                getScheduler().scheduleAlarmAt(slot.name, nextMoment)
            }
            Log.i("ReminderDiag", "Scheduled fallback alarms for all slots")
        }
    }
}
