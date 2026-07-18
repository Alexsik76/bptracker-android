package ua.vn.home.bptracker.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.vn.home.bptracker.data.dto.MeasurementDto

class HomeViewModelTest {

    @Test
    fun `computeHomeState with empty list returns Empty`() {
        val result = HomeViewModel.computeHomeState(emptyList())
        assertTrue(result is HomeState.Empty)
    }

    @Test
    fun `computeHomeState uses parsed time for latest, not raw string sort`() {
        val a = MeasurementDto("a", "2026-07-18T10:00:00+05:00", 120, 80, 70)
        val b = MeasurementDto("b", "2026-07-18T09:00:00+01:00", 130, 85, 75)
        
        val result = HomeViewModel.computeHomeState(listOf(a, b))
        
        assertTrue(result is HomeState.Content)
        val content = result as HomeState.Content
        assertEquals("b", content.latest.id)
    }
    
    @Test
    fun `computeHomeState calculates averages correctly`() {
        val now = java.time.OffsetDateTime.now().toString()
        val m1 = MeasurementDto("1", now, 120, 80, 70)
        val m2 = MeasurementDto("2", now, 140, 90, 80)
        
        val result = HomeViewModel.computeHomeState(listOf(m1, m2))
        
        assertTrue(result is HomeState.Content)
        val content = result as HomeState.Content
        assertEquals(130, content.avgSys)
        assertEquals(85, content.avgDia)
        assertEquals(75, content.avgPulse)
    }
}
