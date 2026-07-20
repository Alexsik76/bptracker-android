package ua.vn.home.bptracker.feature.reminders

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState

class ReminderConfigViewModelTest {

    @Test
    fun `save success transitions Idle to InProgress to Success`() = runTest {
        mockkObject(ServiceLocator)
        val repo = mockk<ua.vn.home.bptracker.data.repository.ReminderConfigRepository>(relaxed = true)
        val settings = mockk<ua.vn.home.bptracker.core.config.SettingsStore>(relaxed = true)
        val scheduler = mockk<ua.vn.home.bptracker.feature.reminders.ReminderScheduler>(relaxed = true)
        
        every { ServiceLocator.reminderConfigRepository } returns repo
        every { ServiceLocator.settingsStore } returns settings
        every { ServiceLocator.reminderScheduler } returns scheduler
        
        every { settings.remindersEnabled } returns flowOf(true)
        
        coEvery { repo.getConfig() } coAnswers { 
            delay(10)
            null 
        }

        val viewModel = ReminderConfigViewModel()
        
        viewModel.state.test {
            // Wait for eventual readiness
            var current = awaitItem()
            while (current.isLoading || current.saveOperation !is OperationUiState.Idle) {
                current = awaitItem()
            }

            viewModel.save()
            
            // Should see InProgress then Success
            assertEquals(OperationUiState.InProgress, awaitItem().saveOperation)
            assertEquals(OperationUiState.Success, awaitItem().saveOperation)
        }
    }
}
