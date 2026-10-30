package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.repository.MeasurementRepository

class MeasurementDetailViewModelTest {

    @Test
    fun `loads measurement from repository using id from SavedStateHandle`() = runTest {
        val repo = mockk<MeasurementRepository>()
        val dto = MeasurementDto("m1", "2026-07-20T10:00:00Z", 120, 80, 70)
        coEvery { repo.getMeasurement("m1") } returns dto

        val handle = SavedStateHandle(mapOf("measurementId" to "m1"))
        val viewModel = MeasurementDetailViewModel(handle, repo)

        viewModel.state.test {
            var item = awaitItem()
            while (item.measurement == null) {
                item = awaitItem()
            }
            assertEquals(dto, item.measurement)
        }
        coVerify { repo.getMeasurement("m1") }
    }

    @Test
    fun `delete calls repository deleteMeasurement and sets deleted to true`() = runTest {
        val repo = mockk<MeasurementRepository>(relaxed = true)
        val dto = MeasurementDto("m1", "2026-07-20T10:00:00Z", 120, 80, 70)
        coEvery { repo.getMeasurement("m1") } returns dto
        coEvery { repo.deleteMeasurement("m1") } just Runs

        val handle = SavedStateHandle(mapOf("measurementId" to "m1"))
        val viewModel = MeasurementDetailViewModel(handle, repo)

        viewModel.state.test {
            var item = awaitItem()
            while (item.measurement == null) {
                item = awaitItem()
            }

            viewModel.delete()

            item = awaitItem()
            if (item.deleting) {
                item = awaitItem()
            }
            assertTrue(item.deleted)
        }
        coVerify { repo.deleteMeasurement("m1") }
    }
}
