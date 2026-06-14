package ua.vn.home.bptracker.feature.ocr

import android.graphics.Bitmap
import kotlinx.coroutines.delay

class MockOcrEngine : OcrEngine {
    override fun warmUp() {
        // No-op for mock
    }

    override suspend fun recognize(bitmap: Bitmap): OcrOutcome {
        delay(1000) // Simulate processing
        return OcrOutcome.Success(
            sys = 120,
            dia = 80,
            pul = 70,
            minConf = 0.95f,
            meanConf = 0.98f
        )
    }
}
