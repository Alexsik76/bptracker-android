package ua.vn.home.bptracker.feature.home

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.feature.reminders.TodaySchedule

class ScheduleViewModelTest {

    @Test
    fun `initial state is skeleton Content`() = runTest {
        mockkObject(ServiceLocator)
        val useCase = mockk<ua.vn.home.bptracker.feature.reminders.TodayScheduleUseCase>(relaxed = true)
        val intakeRepo = mockk<ua.vn.home.bptracker.data.repository.IntakeReportRepository>(relaxed = true)
        val prescriptionRepo = mockk<ua.vn.home.bptracker.data.repository.PrescriptionRepository>(relaxed = true)
        
        every { ServiceLocator.todayScheduleUseCase } returns useCase
        every { ServiceLocator.intakeReportRepository } returns intakeRepo
        every { ServiceLocator.prescriptionRepository } returns prescriptionRepo
        
        val emptySchedule = TodaySchedule(false, "2026-07-20", emptyList())
        every { useCase.observeToday(any()) } returns flowOf(emptySchedule)

        val viewModel = ScheduleViewModel()
        viewModel.state.test {
            val first = awaitItem()
            assertTrue(first is ListUiState.Content)
            assertTrue((first as ListUiState.Content).isRefreshing)
            
            val second = awaitItem()
            assertTrue(second is ListUiState.Content)
            assertEquals(emptySchedule, (second as ListUiState.Content).data)
        }
    }
}
