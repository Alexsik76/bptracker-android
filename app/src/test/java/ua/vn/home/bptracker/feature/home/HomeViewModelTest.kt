package ua.vn.home.bptracker.feature.home

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.MeasurementDto

class HomeViewModelTest {

    @Test
    fun `computeHomeState with empty list returns Empty`() {
        val result = HomeViewModel.computeHomeState(emptyList())
        assertTrue(result is ListUiState.Empty)
    }

    @Test
    fun `computeHomeState uses parsed time for latest, not raw string sort`() {
        val a = MeasurementDto("a", "2026-07-18T10:00:00+05:00", 120, 80, 70)
        val b = MeasurementDto("b", "2026-07-18T09:00:00+01:00", 130, 85, 75)
        
        val result = HomeViewModel.computeHomeState(listOf(a, b))
        
        assertTrue(result is ListUiState.Content)
        val content = (result as ListUiState.Content).data
        assertEquals("b", content.latest.id)
    }
    
    @Test
    fun `computeHomeState calculates averages correctly`() {
        val now = java.time.OffsetDateTime.now().toString()
        val m1 = MeasurementDto("1", now, 120, 80, 70)
        val m2 = MeasurementDto("2", now, 140, 90, 80)
        
        val result = HomeViewModel.computeHomeState(listOf(m1, m2))
        
        assertTrue(result is ListUiState.Content)
        val content = (result as ListUiState.Content).data
        assertEquals(130, content.avgSys)
        assertEquals(85, content.avgDia)
        assertEquals(75, content.avgPulse)
    }

    @Test
    fun `initial state is never Loading`() = runTest {
        mockkObject(ServiceLocator)
        val repo = io.mockk.mockk<ua.vn.home.bptracker.data.repository.MeasurementRepository>(relaxed = true)
        every { ServiceLocator.measurementRepository } returns repo
        every { repo.observeMeasurements() } returns flowOf(emptyList())

        val viewModel = HomeViewModel()
        viewModel.state.test {
            val first = awaitItem()
            assertEquals(ListUiState.Idle, first)
            
            val second = awaitItem()
            assertTrue(second is ListUiState.Empty)
        }
    }
}
