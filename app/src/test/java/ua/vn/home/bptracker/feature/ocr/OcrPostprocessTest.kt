package ua.vn.home.bptracker.feature.ocr

import org.junit.Assert.*
import org.junit.Test

class OcrPostprocessTest {

    @Test
    fun testLetterboxParams() {
        // Sample 1280x720 image to 640 target
        val params = OcrPostprocess.getLetterboxParams(1280, 720, 640)
        assertEquals(0.5f, params.scale, 1e-5f)
        assertEquals(0f, params.padX, 1e-5f)
        assertEquals(140f, params.padY, 1e-5f) // (640 - 360) / 2
    }

    @Test
    fun testNms() {
        val boxes = listOf(
            OcrBox(1, 0.9f, Box(10f, 10f, 20f, 20f)),
            OcrBox(1, 0.8f, Box(12f, 12f, 22f, 22f)), // High overlap with 0.9
            OcrBox(2, 0.7f, Box(50f, 50f, 60f, 60f))  // No overlap
        )
        val selected = OcrPostprocess.nms(boxes, 0.4f)
        assertEquals(2, selected.size)
        assertEquals(0.9f, selected[0].conf, 1e-5f)
        assertEquals(0.7f, selected[1].conf, 1e-5f)
    }

    @Test
    fun testKmeansRows() {
        val boxes = listOf(
            // Row 1 (top)
            OcrBox(1, 0.9f, Box(10f, 10f, 20f, 20f)), // y center 15
            OcrBox(2, 0.9f, Box(30f, 11f, 40f, 21f)), // y center 16
            // Row 2 (middle)
            OcrBox(3, 0.9f, Box(10f, 50f, 20f, 60f)), // y center 55
            // Row 3 (bottom)
            OcrBox(4, 0.9f, Box(10f, 90f, 20f, 100f)), // y center 95
            OcrBox(5, 0.9f, Box(30f, 91f, 40f, 101f))  // y center 96
        )
        val rows = OcrPostprocess.kmeansRows(boxes)
        assertEquals(3, rows.size)
        
        // Check row counts
        assertEquals(2, rows[0].size)
        assertEquals(1, rows[1].size)
        assertEquals(2, rows[2].size)

        // Check horizontal sorting in Row 1
        assertEquals(1, rows[0][0].cls)
        assertEquals(2, rows[0][1].cls)
        
        // Check vertical sorting (kmeans clusters sorted top to bottom)
        assertTrue(rows[0][0].box.cy < rows[1][0].box.cy)
        assertTrue(rows[1][0].box.cy < rows[2][0].box.cy)
    }

    @Test
    fun testAssembleNumber() {
        val row = listOf(
            OcrBox(1, 0.9f, Box(0f, 0f, 10f, 10f)),
            OcrBox(2, 0.9f, Box(15f, 0f, 25f, 10f)),
            OcrBox(5, 0.9f, Box(30f, 0f, 40f, 10f))
        )
        assertEquals(125, OcrPostprocess.assembleNumber(row))
        
        assertEquals(null, OcrPostprocess.assembleNumber(emptyList()))
    }
}
