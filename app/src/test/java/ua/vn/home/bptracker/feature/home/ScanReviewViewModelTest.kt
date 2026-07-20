package ua.vn.home.bptracker.feature.home

import android.graphics.Bitmap
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
import ua.vn.home.bptracker.feature.ocr.OcrOutcome

class ScanReviewViewModelTest {

    @Test
    fun `save success transitions Idle to InProgress to Success`() = runTest {
        mockkObject(ServiceLocator)
        val repo = mockk<ua.vn.home.bptracker.data.repository.MeasurementRepository>(relaxed = true)
        val ocr = mockk<ua.vn.home.bptracker.feature.ocr.OcrEngine>(relaxed = true)
        every { ServiceLocator.measurementRepository } returns repo
        every { ServiceLocator.ocrEngine } returns ocr
        
        coEvery { ocr.recognize(any()) } returns OcrOutcome.Success(120, 80, 70, 0.9f, 0.95f)

        val viewModel = ScanReviewViewModel()
        val bitmap = mockk<Bitmap>()
        viewModel.initWithImage(bitmap)
        
        viewModel.state.test {
            // Wait for ocr to finish and state to become Ready
            var current = awaitItem()
            while (current !is ScanReviewState.Ready) {
                current = awaitItem()
            }
            
            val ready = current as ScanReviewState.Ready
            assertEquals(OperationUiState.Idle, ready.saveOperation)
            
            viewModel.save()
            
            assertEquals(OperationUiState.InProgress, (awaitItem() as ScanReviewState.Ready).saveOperation)
            assertEquals(OperationUiState.Success, (awaitItem() as ScanReviewState.Ready).saveOperation)
        }
    }
}
