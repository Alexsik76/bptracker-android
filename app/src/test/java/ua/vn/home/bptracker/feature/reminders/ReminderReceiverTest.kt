package ua.vn.home.bptracker.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.repository.ReminderConfigRepository
import java.time.LocalDateTime

class ReminderReceiverTest {

    private val testConfig = ReminderConfigDto(
        morningTime = "08:00:00",
        dayTime = "14:00:00",
        eveningTime = "20:00:00",
        maxReminders = 3,
        durationMinutes = 60
    )

    @Test
    fun `exception in resolveConfig still schedules next alarm via fallback`() = runTest {
        val scheduler = mockk<ReminderScheduler>(relaxed = true)
        val repo = mockk<ReminderConfigRepository>(relaxed = true)
        val settings = mockk<SettingsStore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)

        every { intent.action } returns null
        every { intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) } returns "Morning"
        every { settings.remindersEnabled } returns flowOf(true)
        coEvery { repo.resolveConfig() } throws RuntimeException("Network/DB error in resolveConfig")
        coEvery { repo.getCachedConfig() } returns testConfig

        val receiver = ReminderReceiver(
            schedulerOverride = scheduler,
            configRepoOverride = repo,
            settingsStoreOverride = settings
        )

        receiver.processIntent(context, intent)

        coVerify { scheduler.scheduleAlarmAt("Morning", any<LocalDateTime>()) }
    }

    @Test
    fun `Room database failure during today schedule still schedules next alarm`() = runTest {
        val scheduler = mockk<ReminderScheduler>(relaxed = true)
        val repo = mockk<ReminderConfigRepository>(relaxed = true)
        val scheduleUseCase = mockk<TodayScheduleUseCase>(relaxed = true)
        val settings = mockk<SettingsStore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)

        every { intent.action } returns null
        every { intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) } returns "Day"
        every { settings.remindersEnabled } returns flowOf(true)
        coEvery { repo.resolveConfig() } throws RuntimeException("resolveConfig error")
        coEvery { scheduleUseCase.getTodayOnce(any()) } throws RuntimeException("Room SQLite disk I/O error")
        coEvery { repo.getCachedConfig() } returns testConfig

        val receiver = ReminderReceiver(
            schedulerOverride = scheduler,
            configRepoOverride = repo,
            todayScheduleOverride = scheduleUseCase,
            settingsStoreOverride = settings
        )

        receiver.processIntent(context, intent)

        coVerify { scheduler.scheduleAlarmAt("Day", any<LocalDateTime>()) }
    }

    @Test
    fun `invalid period string still schedules fallback without crash`() = runTest {
        val scheduler = mockk<ReminderScheduler>(relaxed = true)
        val repo = mockk<ReminderConfigRepository>(relaxed = true)
        val settings = mockk<SettingsStore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)

        every { intent.action } returns null
        every { intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) } returns "UnknownPeriod"
        every { settings.remindersEnabled } returns flowOf(true)
        coEvery { repo.getCachedConfig() } returns testConfig

        val receiver = ReminderReceiver(
            schedulerOverride = scheduler,
            configRepoOverride = repo,
            settingsStoreOverride = settings
        )

        receiver.processIntent(context, intent)

        coVerify { scheduler.scheduleAlarmAt("UnknownPeriod", any<LocalDateTime>()) }
    }

    @Test
    fun `CancellationException is rethrown by processIntent`() = runTest {
        val scheduler = mockk<ReminderScheduler>(relaxed = true)
        val repo = mockk<ReminderConfigRepository>(relaxed = true)
        val settings = mockk<SettingsStore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)

        every { intent.action } returns null
        every { intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) } returns "Morning"
        every { settings.remindersEnabled } returns flowOf(true)
        coEvery { repo.resolveConfig() } throws CancellationException("Coroutines cancelled")

        val receiver = ReminderReceiver(
            schedulerOverride = scheduler,
            configRepoOverride = repo,
            settingsStoreOverride = settings
        )

        var cancellationThrown = false
        try {
            receiver.processIntent(context, intent)
        } catch (e: CancellationException) {
            cancellationThrown = true
        }
        assertTrue(cancellationThrown)
    }

    @Test
    fun `finish is called on PendingResult even when processIntent throws`() = runTest {
        val scheduler = mockk<ReminderScheduler>(relaxed = true)
        val repo = mockk<ReminderConfigRepository>(relaxed = true)
        val settings = mockk<SettingsStore>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val intent = mockk<Intent>(relaxed = true)
        val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)

        every { intent.action } returns null
        every { intent.getStringExtra(NotificationHelper.EXTRA_PERIOD) } returns "Morning"
        every { settings.remindersEnabled } returns flowOf(true)
        coEvery { repo.resolveConfig() } throws RuntimeException("Fatal error")

        val receiver = spyk(
            ReminderReceiver(
                schedulerOverride = scheduler,
                configRepoOverride = repo,
                settingsStoreOverride = settings,
                coroutineScopeOverride = this
            )
        )
        every { receiver.goAsync() } returns pendingResult

        receiver.onReceive(context, intent)
        testScheduler.advanceUntilIdle()

        verify { pendingResult.finish() }
    }
}
