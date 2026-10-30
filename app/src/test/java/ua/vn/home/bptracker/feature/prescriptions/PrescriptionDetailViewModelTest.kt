package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
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
import ua.vn.home.bptracker.data.repository.PrescriptionRepository

class PrescriptionDetailViewModelTest {

    @Test
    fun `initial state is skeleton Content`() = runTest {
        mockkObject(ServiceLocator)
        val repository = mockk<PrescriptionRepository>(relaxed = true)
        every { ServiceLocator.prescriptionRepository } returns repository
        every { repository.getPrescriptions() } returns flowOf(emptyList())

        val viewModel = PrescriptionDetailViewModel()
        viewModel.state.test {
            val first = awaitItem()
            assertTrue(first is ListUiState.Content)
            assertTrue((first as ListUiState.Content).isRefreshing)
            assertEquals(PrescriptionDetailPayload.EMPTY, first.data)
        }
    }

    @Test
    fun `deletePrescription calls repository and sets deleted to true`() = runTest {
        mockkObject(ServiceLocator)
        val repository = mockk<PrescriptionRepository>(relaxed = true)
        every { ServiceLocator.prescriptionRepository } returns repository
        coEvery { repository.deletePrescription("p1") } just Runs
        every { repository.getPrescriptions() } returns flowOf(listOf(PrescriptionReadDto("p1", "Dr. House", "2026-07-20", true, "now")))
        every { repository.getItems("p1") } returns flowOf(emptyList())

        val handle = SavedStateHandle(mapOf("prescriptionId" to "p1"))
        val viewModel = PrescriptionDetailViewModel(handle, repository)

        viewModel.deleted.test {
            assertEquals(false, awaitItem())
            viewModel.deletePrescription()
            assertEquals(true, awaitItem())
        }

        coVerify { repository.deletePrescription("p1") }
    }
}
