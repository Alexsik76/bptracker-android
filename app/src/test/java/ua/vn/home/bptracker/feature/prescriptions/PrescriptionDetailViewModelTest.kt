package ua.vn.home.bptracker.feature.prescriptions

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState

class PrescriptionDetailViewModelTest {

    @Test
    fun `initial state is never Loading`() = runTest {
        mockkObject(ServiceLocator)
        val repository = mockk<ua.vn.home.bptracker.data.repository.PrescriptionRepository>(relaxed = true)
        every { ServiceLocator.prescriptionRepository } returns repository
        every { repository.getPrescriptions() } returns flowOf(emptyList())

        val viewModel = PrescriptionDetailViewModel()
        viewModel.state.test {
            val first = awaitItem()
            assertNotEquals(ListUiState.Loading, first)
            assertTrue(first is ListUiState.Empty)
        }
    }
}
