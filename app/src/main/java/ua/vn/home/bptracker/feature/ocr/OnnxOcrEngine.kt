package ua.vn.home.bptracker.feature.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class OnnxOcrEngine(private val context: Context) : OcrEngine {

    private val ortEnv = OrtEnvironment.getEnvironment()
    private val displaySession: OrtSession by lazy {
        val model = context.assets.open("models/display_detector_int8.onnx").readBytes()
        ortEnv.createSession(model)
    }
    private val digitSession: OrtSession by lazy {
        val model = context.assets.open("models/digit_detector_int8.onnx").readBytes()
        ortEnv.createSession(model)
    }

    override fun warmUp() {
        // Touch both lazy sessions to trigger eager model loading
        try {
            val d1 = displaySession.inputNames
            val d2 = digitSession.inputNames
        } catch (e: Exception) {
            // Warm-up is best effort; don't crash app launch
        }
    }

    override suspend fun recognize(bitmap: Bitmap): OcrOutcome = withContext(Dispatchers.Default) {
        try {
            // Stage 1: Detect Display
            val displayBoxes = runDetection(displaySession, bitmap, 640, 1, 0.05f)
            if (displayBoxes.isEmpty()) return@withContext OcrOutcome.Failure("display-not-found")

            val bestDisplay = displayBoxes.maxBy { it.conf }
            val croppedBitmap = cropDisplay(bitmap, bestDisplay.box)

            // Stage 2: Detect Digits
            val digitBoxes = runDetection(digitSession, croppedBitmap, 480, 10, 0.25f)
            if (digitBoxes.isEmpty()) return@withContext OcrOutcome.Failure("digits-not-found")

            val nmsBoxes = OcrPostprocess.classAgnosticNms(digitBoxes, 0.4f)
            val rows = OcrPostprocess.kmeansRows(nmsBoxes)

            if (rows.size != 3) return@withContext OcrOutcome.Failure("wrong-row-count")

            val sys = OcrPostprocess.assembleNumber(rows[0])
            val dia = OcrPostprocess.assembleNumber(rows[1])
            val pul = OcrPostprocess.assembleNumber(rows[2])

            if (sys == null || dia == null || pul == null) {
                return@withContext OcrOutcome.Failure("assemble-failed")
            }

            val allConfs = nmsBoxes.map { it.conf }
            OcrOutcome.Success(
                sys = sys, dia = dia, pul = pul,
                minConf = allConfs.min(),
                meanConf = allConfs.average().toFloat()
            )
        } catch (e: Exception) {
            OcrOutcome.Failure("ocr-error: ${e.message}")
        }
    }

    private fun runDetection(
        session: OrtSession,
        bitmap: Bitmap,
        targetSize: Int,
        numClasses: Int,
        confThreshold: Float
    ): List<OcrBox> {
        val params = OcrPostprocess.getLetterboxParams(bitmap.width, bitmap.height, targetSize)
        val tensor = bitmapToTensor(bitmap, targetSize, params)

        return tensor.use { t ->
            val inputName = session.inputNames.first()
            session.run(mapOf(inputName to t)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val output = results[0].value as Array<Array<FloatArray>> // [1, 4+numClasses, numAnchors]

                // Flatten output for postprocessor
                val flatOutput = flattenYoloOutput(output[0])
                val numAnchors = flatOutput.size / (4 + numClasses)

                OcrPostprocess.decodeOutput(flatOutput, numClasses, numAnchors, confThreshold, params)
            }
        }
    }

    private fun bitmapToTensor(bitmap: Bitmap, targetSize: Int, params: LetterboxParams): OnnxTensor {
        val letterboxed = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))

        val dstW = (bitmap.width * params.scale).toInt()
        val dstH = (bitmap.height * params.scale).toInt()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.Rect(
            params.padX.toInt(),
            params.padY.toInt(),
            (params.padX + dstW).toInt(),
            (params.padY + dstH).toInt()
        )
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

        val floatBuffer = FloatBuffer.allocate(3 * targetSize * targetSize)
        val pixels = IntArray(targetSize * targetSize)
        letterboxed.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // CHW format
        for (c in 0 until 3) {
            for (p in pixels) {
                val value = when (c) {
                    0 -> (p shr 16) and 0xFF // R
                    1 -> (p shr 8) and 0xFF  // G
                    else -> p and 0xFF       // B
                }
                floatBuffer.put(value / 255.0f)
            }
        }
        floatBuffer.rewind()

        return OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 3, targetSize.toLong(), targetSize.toLong()))
    }

    private fun flattenYoloOutput(data: Array<FloatArray>): FloatArray {
        val rows = data.size
        val cols = data[0].size
        val flat = FloatArray(rows * cols)
        for (i in 0 until rows) {
            System.arraycopy(data[i], 0, flat, i * cols, cols)
        }
        return flat
    }

    private fun cropDisplay(bitmap: Bitmap, box: Box): Bitmap {
        val marginW = box.width * 0.03f
        val marginH = box.height * 0.03f
        
        val x1 = (box.x1 - marginW).toInt().coerceIn(0, bitmap.width - 1)
        val y1 = (box.y1 - marginH).toInt().coerceIn(0, bitmap.height - 1)
        val x2 = (box.x2 + marginW).toInt().coerceIn(x1 + 1, bitmap.width)
        val y2 = (box.y2 + marginH).toInt().coerceIn(y1 + 1, bitmap.height)
        
        return Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)
    }
}
