package ua.vn.home.bptracker.feature.ocr

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

data class Box(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    val width get() = x2 - x1
    val height get() = y2 - y1
    val cx get() = x1 + width / 2
    val cy get() = y1 + height / 2
}

data class OcrBox(
    val cls: Int,
    val conf: Float,
    val box: Box
)

data class LetterboxParams(
    val scale: Float,
    val padX: Float,
    val padY: Float
)

object OcrPostprocess {

    fun getLetterboxParams(srcW: Int, srcH: Int, targetSize: Int): LetterboxParams {
        val scale = min(targetSize.toFloat() / srcW, targetSize.toFloat() / srcH)
        val newW = (srcW * scale).roundToInt()
        val newH = (srcH * scale).roundToInt()
        val padX = floor((targetSize - newW) / 2f)
        val padY = floor((targetSize - newH) / 2f)
        return LetterboxParams(scale, padX, padY)
    }

    /**
     * Decodes YOLOv8 output (1, 4+numClasses, numAnchors) in channel-major format.
     * Maps coordinates back to original image space using LetterboxParams.
     */
    fun decodeOutput(
        data: FloatArray,
        numClasses: Int,
        numAnchors: Int,
        confThreshold: Float,
        params: LetterboxParams
    ): List<OcrBox> {
        val boxes = mutableListOf<OcrBox>()
        val A = numAnchors

        for (i in 0 until A) {
            var maxConf = -1f
            var maxCls = -1

            for (c in 0 until numClasses) {
                val conf = data[(4 + c) * A + i]
                if (conf > maxConf) {
                    maxConf = conf
                    maxCls = c
                }
            }

            if (maxConf >= confThreshold) {
                val cx = data[0 * A + i]
                val cy = data[1 * A + i]
                val w = data[2 * A + i]
                val h = data[3 * A + i]

                val x1 = (cx - w / 2f - params.padX) / params.scale
                val y1 = (cy - h / 2f - params.padY) / params.scale
                val x2 = (cx + w / 2f - params.padX) / params.scale
                val y2 = (cy + h / 2f - params.padY) / params.scale

                boxes.add(OcrBox(maxCls, maxConf, Box(x1, y1, x2, y2)))
            }
        }
        return boxes
    }

    fun nms(boxes: List<OcrBox>, iouThreshold: Float): List<OcrBox> {
        val sorted = boxes.sortedByDescending { it.conf }.toMutableList()
        val selected = mutableListOf<OcrBox>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            // Class-agnostic NMS to avoid overlapping digits of different classes
            sorted.removeAll { it.calculateIoU(best) > iouThreshold }
        }
        return selected
    }

    private fun OcrBox.calculateIoU(other: OcrBox): Float {
        val b1 = this.box
        val b2 = other.box
        
        val xI1 = maxOf(b1.x1, b2.x1)
        val yI1 = maxOf(b1.y1, b2.y1)
        val xI2 = minOf(b1.x2, b2.x2)
        val yI2 = minOf(b1.y2, b2.y2)
        
        val interW = maxOf(0f, xI2 - xI1)
        val interH = maxOf(0f, yI2 - yI1)
        val interArea = interW * interH
        
        val area1 = b1.width * b1.height
        val area2 = b2.width * b2.height
        
        return interArea / (area1 + area2 - interArea)
    }

    /**
     * Groups boxes into 3 rows using Lloyd's k-means on center-Y.
     * Improved stability for digit detection.
     */
    fun kmeansRows(boxes: List<OcrBox>): List<List<OcrBox>> {
        if (boxes.isEmpty()) return emptyList()

        // If very few boxes, just return them as sorted by Y (effectively one or more rows)
        if (boxes.size < 2) {
            return listOf(boxes.sortedBy { it.box.x1 })
        }

        val yMin = boxes.minOf { it.box.cy }
        val yMax = boxes.maxOf { it.box.cy }
        val yRange = yMax - yMin

        // Initial centers: spread them out
        var centers = floatArrayOf(
            yMin + yRange * 0.10f, 
            yMin + yRange * 0.50f, 
            yMin + yRange * 0.90f
        )
        
        repeat(20) {
            val clusters = Array(3) { mutableListOf<OcrBox>() }
            for (box in boxes) {
                val cy = box.box.cy
                val nearestIdx = centers.indices.minByOrNull { kotlin.math.abs(centers[it] - cy) } ?: 0
                clusters[nearestIdx].add(box)
            }

            val newCenters = FloatArray(3)
            var converged = true
            for (i in 0 until 3) {
                if (clusters[i].isEmpty()) {
                    newCenters[i] = centers[i]
                } else {
                    newCenters[i] = clusters[i].map { it.box.cy }.average().toFloat()
                    if (kotlin.math.abs(newCenters[i] - centers[i]) > 0.1f) {
                        converged = false
                    }
                }
            }
            centers = newCenters
            if (converged) return@repeat
        }

        val finalClusters = Array(3) { mutableListOf<OcrBox>() }
        for (box in boxes) {
            val cy = box.box.cy
            val nearestIdx = centers.indices.minByOrNull { kotlin.math.abs(centers[it] - cy) } ?: 0
            finalClusters[nearestIdx].add(box)
        }

        return finalClusters
            .filter { it.isNotEmpty() }
            .sortedBy { row -> row.map { it.box.cy }.average() } // top to bottom
            .map { row -> row.sortedBy { it.box.x1 } } // left to right
    }

    fun assembleNumber(row: List<OcrBox>): Int? {
        if (row.isEmpty()) return null
        val s = row.joinToString("") { it.cls.toString() }
        return s.toIntOrNull()
    }
}
