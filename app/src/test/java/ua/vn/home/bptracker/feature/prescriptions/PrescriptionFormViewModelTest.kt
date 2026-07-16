package ua.vn.home.bptracker.feature.prescriptions

import org.junit.Assert.*
import org.junit.Test

class PrescriptionFormViewModelTest {

    @Test
    fun testStateValidation() {
        val emptyState = PrescriptionFormState()
        assertFalse(emptyState.isValid)

        val validState = PrescriptionFormState(doctor = "Dr. House", prescribedOn = "2024-01-01")
        assertTrue(validState.isValid)

        assertFalse(validState.copy(doctor = "").isValid)
        assertFalse(validState.copy(prescribedOn = "").isValid)
    }

    @Test
    fun testSavedIdCapture() {
        // This is a unit test for the state logic. 
        // In the ViewModel, we updated it to hold savedId.
        val state = PrescriptionFormState(isSaved = true, savedId = "new-id-123")
        assertTrue(state.isSaved)
        assertEquals("new-id-123", state.savedId)
        
        val editState = PrescriptionFormState(isSaved = true, savedId = null)
        assertTrue(editState.isSaved)
        assertNull(editState.savedId)
    }
}
