package ua.vn.home.bptracker.feature.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState

class ManualEntryViewModelTest {

    @Test
    fun `save success transitions Idle to InProgress to Success`() = runTest {
        mockkObject(ServiceLocator)
        val repo = mockk<ua.vn.home.bptracker.data.repository.MeasurementRepository>(relaxed = true)
        every { ServiceLocator.measurementRepository } returns repo
        
        val viewModel = ManualEntryViewModel()
        viewModel.onSysChange("120")
        viewModel.onDiaChange("80")
        viewModel.onPulseChange("70")
        
        viewModel.state.test {
            // Initial state from init/changes
            assertEquals(OperationUiState.Idle, awaitItem().saveOperation)
            
            viewModel.save()
            
            assertEquals(OperationUiState.InProgress, awaitItem().saveOperation)
            assertEquals(OperationUiState.Success, awaitItem().saveOperation)
        }
    }

    @Test
    fun `save failure transitions Idle to InProgress to Error`() = runTest {
        mockkObject(ServiceLocator)
        val repo = mockk<ua.vn.home.bptracker.data.repository.MeasurementRepository>()
        every { ServiceLocator.measurementRepository } returns repo
        coEvery { repo.createMeasurement(any(), any(), any()) } throws RuntimeException("Network error")

        val viewModel = ManualEntryViewModel()
        viewModel.onSysChange("120")
        viewModel.onDiaChange("80")
        viewModel.onPulseChange("70")

        viewModel.state.test {
            assertEquals(OperationUiState.Idle, awaitItem().saveOperation)
            
            viewModel.save()
            
            assertEquals(OperationUiState.InProgress, awaitItem().saveOperation)
            val errorState = awaitItem().saveOperation
            assertTrue(errorState is OperationUiState.Error)
            assertEquals("Network error", (errorState as OperationUiState.Error).message)
        }
    }
}
