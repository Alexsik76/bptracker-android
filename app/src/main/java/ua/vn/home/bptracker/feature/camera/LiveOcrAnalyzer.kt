package ua.vn.home.bptracker.feature.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.runBlocking
import ua.vn.home.bptracker.feature.ocr.AcceptedReading
import ua.vn.home.bptracker.feature.ocr.Consensus
import ua.vn.home.bptracker.feature.ocr.LiveOcrStabilizer
import ua.vn.home.bptracker.feature.ocr.LiveOcrVerdict
import ua.vn.home.bptracker.feature.ocr.OcrEngine
import ua.vn.home.bptracker.feature.ocr.OcrOutcome
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class LiveOcrSessionStats(
    val framesDelivered: Int,
    val framesAnalysed: Int,
    val framesDropped: Int?,
    val verdictCounts: Map<String, Int>,
    val failureReasonCounts: Map<String, Int>,
    val avgRecognizeTimeMs: Double,
    val maxRecognizeTimeMs: Long,
    val timeToConsensusMs: Long?,
    val finalValues: Consensus?,
)

data class LiveOcrSession(
    val bitmap: Bitmap,
    val consensus: Consensus,
    val stats: LiveOcrSessionStats,
) {
    val sys: Int get() = consensus.sys
    val dia: Int get() = consensus.dia
    val pul: Int get() = consensus.pul
}

class LiveOcrAnalyzer(
    private val ocrEngine: OcrEngine,
    private val stabilizer: LiveOcrStabilizer = LiveOcrStabilizer(),
    private val onProgress: (minVotes: Int) -> Unit = {},
    private val onConsensus: (session: LiveOcrSession) -> Unit,
) : ImageAnalysis.Analyzer {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val framesDeliveredCount = AtomicInteger(0)
    private val framesAnalysedCount = AtomicInteger(0)
    private val estimatedDroppedByCameraX = AtomicInteger(0)
    private var lastFrameTimestampNs: Long = 0L

    private val hasDeliveredResult = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    private val sessionStartTime = SystemClock.uptimeMillis()
    private val recognizeTimes = Collections.synchronizedList(mutableListOf<Long>())
    private val verdictCounts = ConcurrentHashMap<String, Int>()
    private val failureReasonCounts = ConcurrentHashMap<String, Int>()

    private data class StoredFrame(val reading: AcceptedReading, val bitmap: Bitmap)
    private val storedFrames = Collections.synchronizedList(mutableListOf<StoredFrame>())

    override fun analyze(image: ImageProxy) {
        val delivered = framesDeliveredCount.incrementAndGet()
        val currentTimestampNs = image.imageInfo.timestamp
        if (lastFrameTimestampNs > 0L) {
            val deltaNs = currentTimestampNs - lastFrameTimestampNs
            val nominalIntervalNs = 33_333_333L // ~30 fps
            if (deltaNs > nominalIntervalNs * 1.5) {
                val droppedFrames = ((deltaNs.toDouble() / nominalIntervalNs) - 1.0).toInt().coerceAtLeast(0)
                estimatedDroppedByCameraX.addAndGet(droppedFrames)
            }
        }
        lastFrameTimestampNs = currentTimestampNs

        if (isStopped.get() || hasDeliveredResult.get()) {
            image.close()
            return
        }

        val frameNumber = framesAnalysedCount.incrementAndGet()
        val rotated: Bitmap
        try {
            val rotation = image.imageInfo.rotationDegrees
            val cropRect = image.cropRect
            val fullBitmap = image.toBitmap()
            image.close()

            val viewPortCropped = if (cropRect.left >= 0 && cropRect.top >= 0 &&
                cropRect.right <= fullBitmap.width && cropRect.bottom <= fullBitmap.height &&
                cropRect.width() > 0 && cropRect.height() > 0 &&
                (cropRect.width() != fullBitmap.width || cropRect.height() != fullBitmap.height)
            ) {
                val cropped = Bitmap.createBitmap(
                    fullBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                )
                if (cropped !== fullBitmap) {
                    fullBitmap.recycle()
                }
                cropped
            } else {
                fullBitmap
            }

            rotated = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rot = Bitmap.createBitmap(
                    viewPortCropped,
                    0,
                    0,
                    viewPortCropped.width,
                    viewPortCropped.height,
                    matrix,
                    true,
                )
                if (rot !== viewPortCropped) {
                    viewPortCropped.recycle()
                }
                rot
            } else {
                viewPortCropped
            }
        } catch (_: Exception) {
            try { image.close() } catch (_: Exception) {}
            return
        }

        val ocrStartTime = SystemClock.uptimeMillis()
        val outcome = try {
            runBlocking { ocrEngine.recognize(rotated) }
        } catch (e: Exception) {
            OcrOutcome.Failure("exception: ${e.message}")
        }
        val ocrDuration = SystemClock.uptimeMillis() - ocrStartTime
        recognizeTimes.add(ocrDuration)

        val result = when (outcome) {
            is OcrOutcome.Success -> stabilizer.submitReading(
                sys = outcome.sys,
                dia = outcome.dia,
                pul = outcome.pul,
                minConf = outcome.minConf,
                meanConf = outcome.meanConf,
            )
            is OcrOutcome.Failure -> {
                failureReasonCounts.compute(outcome.reason) { _, count -> (count ?: 0) + 1 }
                stabilizer.submitEmpty(outcome.reason)
            }
        }

        verdictCounts.compute(result.verdict.name) { _, count -> (count ?: 0) + 1 }
        if (result.verdict is LiveOcrVerdict.Rejected) {
            failureReasonCounts.compute(result.verdict.reason) { _, count -> (count ?: 0) + 1 }
        }

        mainHandler.post {
            onProgress(result.minVotes)
        }

        when (result.verdict) {
            is LiveOcrVerdict.Accepted -> {
                synchronized(storedFrames) {
                    val acceptedReading = result.window.last()
                    storedFrames.add(StoredFrame(acceptedReading, rotated))
                    while (storedFrames.size > stabilizer.windowSize) {
                        val evicted = storedFrames.removeAt(0)
                        evicted.bitmap.recycle()
                    }
                }
            }
            is LiveOcrVerdict.Reset -> {
                synchronized(storedFrames) {
                    for (f in storedFrames) {
                        f.bitmap.recycle()
                    }
                    storedFrames.clear()
                }
                rotated.recycle()
            }
            is LiveOcrVerdict.Rejected, is LiveOcrVerdict.Empty -> {
                rotated.recycle()
            }
        }

        if (result.isConsensus && hasDeliveredResult.compareAndSet(false, true)) {
            val consensus = result.consensus!!
            val agreedFrame = synchronized(storedFrames) {
                storedFrames.lastOrNull {
                    it.reading.sys == consensus.sys &&
                    it.reading.dia == consensus.dia &&
                    it.reading.pul == consensus.pul
                } ?: storedFrames.lastOrNull()
            }
            val consensusBitmap = agreedFrame?.bitmap ?: rotated

            synchronized(storedFrames) {
                for (f in storedFrames) {
                    if (f.bitmap !== consensusBitmap) {
                        f.bitmap.recycle()
                    }
                }
                storedFrames.clear()
            }

            isStopped.set(true)
            val timeToConsensus = SystemClock.uptimeMillis() - sessionStartTime
            val stats = buildStats(timeToConsensus = timeToConsensus, consensus = consensus)

            val session = LiveOcrSession(
                bitmap = consensusBitmap,
                consensus = consensus,
                stats = stats,
            )

            mainHandler.post {
                onConsensus(session)
            }
        }
    }

    fun stop() {
        if (isStopped.compareAndSet(false, true)) {
            synchronized(storedFrames) {
                for (frame in storedFrames) {
                    frame.bitmap.recycle()
                }
                storedFrames.clear()
            }
        }
    }

    private fun buildStats(timeToConsensus: Long?, consensus: Consensus?): LiveOcrSessionStats {
        val delivered = framesDeliveredCount.get()
        val analysed = framesAnalysedCount.get()
        val estimatedDropped = estimatedDroppedByCameraX.get()
        val queueDropped = (delivered - analysed).coerceAtLeast(0)
        val dropped = if (estimatedDropped > 0) estimatedDropped else queueDropped
        val avgTime = if (recognizeTimes.isEmpty()) 0.0 else recognizeTimes.average()
        val maxTime = recognizeTimes.maxOrNull() ?: 0L

        return LiveOcrSessionStats(
            framesDelivered = delivered,
            framesAnalysed = analysed,
            framesDropped = dropped,
            verdictCounts = verdictCounts.toMap(),
            failureReasonCounts = failureReasonCounts.toMap(),
            avgRecognizeTimeMs = avgTime,
            maxRecognizeTimeMs = maxTime,
            timeToConsensusMs = timeToConsensus,
            finalValues = consensus,
        )
    }
}
