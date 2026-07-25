package ua.vn.home.bptracker.feature.prescriptions

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
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

class PrescriptionsViewModelTest {

    @Test
    fun `initial state is skeleton Content`() = runTest {
        mockkObject(ServiceLocator)
        val repository = mockk<ua.vn.home.bptracker.data.repository.PrescriptionRepository>(relaxed = true)
        every { ServiceLocator.prescriptionRepository } returns repository
        every { repository.getPrescriptions() } returns flowOf(emptyList())

        val viewModel = PrescriptionsViewModel()
        viewModel.state.test {
            val first = awaitItem()
            assertTrue(first is ListUiState.Content)
            assertTrue((first as ListUiState.Content).isRefreshing)
            assertEquals(emptyList<PrescriptionReadDto>(), first.data)
            
            val second = awaitItem()
            assertTrue(second is ListUiState.Empty)
        }
    }
}
