package ua.vn.home.bptracker.feature.prescriptions

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.data.dto.WhenSlot

class MedicationItemFormViewModelTest {

    @Test
    fun `save success transitions Idle to InProgress to Success`() = runTest {
        mockkObject(ServiceLocator)
        val repo = mockk<ua.vn.home.bptracker.data.repository.PrescriptionRepository>(relaxed = true)
        every { ServiceLocator.prescriptionRepository } returns repo
        
        val viewModel = MedicationItemFormViewModel()
        viewModel.init("p1", null)
        viewModel.onMedicineChange("Aspirin")
        viewModel.onWhenSlotsChange(WhenSlot.Morning, true)
        viewModel.onDoseAmountChange("100")
        
        viewModel.state.test {
            assertEquals(OperationUiState.Idle, awaitItem().saveOperation)
            
            viewModel.save()
            
            assertEquals(OperationUiState.InProgress, awaitItem().saveOperation)
            assertEquals(OperationUiState.Success, awaitItem().saveOperation)
        }
    }
}
