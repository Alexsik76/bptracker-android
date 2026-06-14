package ua.vn.home.bptracker.feature.ocr

import android.graphics.Bitmap

sealed interface OcrOutcome {
    data class Success(
        val sys: Int,
        val dia: Int,
        val pul: Int,
        val minConf: Float,
        val meanConf: Float,
        val previewCrop: Bitmap? = null
    ) : OcrOutcome

    data class Failure(val reason: String) : OcrOutcome
}

interface OcrEngine {
    fun warmUp()
    suspend fun recognize(bitmap: Bitmap): OcrOutcome
    suspend fun recognizeRemote(bitmap: Bitmap): OcrOutcome
}
