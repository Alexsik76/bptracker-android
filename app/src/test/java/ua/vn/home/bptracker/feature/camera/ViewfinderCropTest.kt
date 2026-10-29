package ua.vn.home.bptracker.feature.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewfinderCropTest {

    @Test
    fun testCentredResult() {
        val imageWidth = 1000
        val imageHeight = 800
        val viewWidthPx = 500f
        val viewHeightPx = 400f
        val frameWidthPx = 200f
        val frameHeightPx = 150f
        val margin = 0.10f

        val rect = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            frameWidthPx = frameWidthPx,
            frameHeightPx = frameHeightPx,
            margin = margin,
        )

        val leftMargin = rect.left
        val rightMargin = imageWidth - rect.right
        val topMargin = rect.top
        val bottomMargin = imageHeight - rect.bottom

        assertEquals(leftMargin, rightMargin)
        assertEquals(topMargin, bottomMargin)
        assertEquals(imageWidth / 2, rect.left + rect.width / 2)
        assertEquals(imageHeight / 2, rect.top + rect.height / 2)
    }

    @Test
    fun testMarginApplied() {
        val imageWidth = 1000
        val imageHeight = 1000
        val viewWidthPx = 1000f
        val viewHeightPx = 1000f
        val frameWidthPx = 200f
        val frameHeightPx = 150f

        val noMarginRect = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            frameWidthPx = frameWidthPx,
            frameHeightPx = frameHeightPx,
            margin = 0f,
        )

        assertEquals(200, noMarginRect.width)
        assertEquals(150, noMarginRect.height)

        val marginRect = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            frameWidthPx = frameWidthPx,
            frameHeightPx = frameHeightPx,
            margin = 0.10f,
        )

        // Frame expanded by 10% on each side -> 1.20x original frame dimensions
        assertEquals(240, marginRect.width)
        assertEquals(180, marginRect.height)
        assertEquals((1000 - 240) / 2, marginRect.left)
        assertEquals((1000 - 180) / 2, marginRect.top)
    }

    @Test
    fun testClampingWhenExpandedFrameExceedsImage() {
        // Both dimensions exceed image
        val imageWidth = 500
        val imageHeight = 400
        val rectBothExceed = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = 500f,
            viewHeightPx = 400f,
            frameWidthPx = 480f,
            frameHeightPx = 380f,
            margin = 0.10f, // 480 * 1.2 = 576 > 500, 380 * 1.2 = 456 > 400
        )

        assertEquals(0, rectBothExceed.left)
        assertEquals(0, rectBothExceed.top)
        assertEquals(imageWidth, rectBothExceed.width)
        assertEquals(imageHeight, rectBothExceed.height)
        assertEquals(imageWidth, rectBothExceed.right)
        assertEquals(imageHeight, rectBothExceed.bottom)

        // Only width exceeds image
        val rectWidthExceeds = viewfinderCropRect(
            imageWidth = 500,
            imageHeight = 1000,
            viewWidthPx = 500f,
            viewHeightPx = 1000f,
            frameWidthPx = 480f,
            frameHeightPx = 200f,
            margin = 0.10f, // width = 576 > 500, height = 240 <= 1000
        )

        assertEquals(0, rectWidthExceeds.left)
        assertEquals(500, rectWidthExceeds.width)
        assertEquals(240, rectWidthExceeds.height)
        assertEquals((1000 - 240) / 2, rectWidthExceeds.top)

        // Only height exceeds image
        val rectHeightExceeds = viewfinderCropRect(
            imageWidth = 1000,
            imageHeight = 500,
            viewWidthPx = 1000f,
            viewHeightPx = 500f,
            frameWidthPx = 200f,
            frameHeightPx = 450f,
            margin = 0.10f, // width = 240 <= 1000, height = 540 > 500
        )

        assertEquals(0, rectHeightExceeds.top)
        assertEquals(500, rectHeightExceeds.height)
        assertEquals(240, rectHeightExceeds.width)
        assertEquals((1000 - 240) / 2, rectHeightExceeds.left)
    }

    @Test
    fun testNonSquareViewImageRatios() {
        val imageWidth = 1200
        val imageHeight = 1600
        val viewWidthPx = 600f
        val viewHeightPx = 1000f
        val frameWidthPx = 200f
        val frameHeightPx = 200f
        val margin = 0.10f

        // scaleX = 1200 / 600 = 2.0
        // scaleY = 1600 / 1000 = 1.6
        // expandedWidth = 200 * 1.2 = 240 -> image width = 240 * 2.0 = 480
        // expandedHeight = 200 * 1.2 = 240 -> image height = 240 * 1.6 = 384
        val rect = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = viewWidthPx,
            viewHeightPx = viewHeightPx,
            frameWidthPx = frameWidthPx,
            frameHeightPx = frameHeightPx,
            margin = margin,
        )

        assertEquals(480, rect.width)
        assertEquals(384, rect.height)
        assertEquals((1200 - 480) / 2, rect.left)
        assertEquals((1600 - 384) / 2, rect.top)
        assertEquals(rect.left + rect.width, rect.right)
        assertEquals(rect.top + rect.height, rect.bottom)
    }

    @Test
    fun testDegenerateInputsZeroViewSizeReturnsFullImage() {
        val imageWidth = 1920
        val imageHeight = 1080

        // Zero view width and height
        val zeroBoth = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = 0f,
            viewHeightPx = 0f,
            frameWidthPx = 280f,
            frameHeightPx = 220f,
            margin = 0.10f,
        )
        assertEquals(CropRect(0, 0, imageWidth, imageHeight), zeroBoth)

        // Zero view width
        val zeroWidth = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = 0f,
            viewHeightPx = 1080f,
            frameWidthPx = 280f,
            frameHeightPx = 220f,
            margin = 0.10f,
        )
        assertEquals(CropRect(0, 0, imageWidth, imageHeight), zeroWidth)

        // Zero view height
        val zeroHeight = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = 1920f,
            viewHeightPx = 0f,
            frameWidthPx = 280f,
            frameHeightPx = 220f,
            margin = 0.10f,
        )
        assertEquals(CropRect(0, 0, imageWidth, imageHeight), zeroHeight)

        // Negative view dimensions
        val negativeView = viewfinderCropRect(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidthPx = -500f,
            viewHeightPx = -300f,
            frameWidthPx = 280f,
            frameHeightPx = 220f,
            margin = 0.10f,
        )
        assertEquals(CropRect(0, 0, imageWidth, imageHeight), negativeView)
    }

    @Test
    fun testReturnsNonEmptyRectForTinyImage() {
        val rect = viewfinderCropRect(
            imageWidth = 1,
            imageHeight = 1,
            viewWidthPx = 100f,
            viewHeightPx = 100f,
            frameWidthPx = 10f,
            frameHeightPx = 10f,
            margin = 0.10f,
        )
        assertTrue("Width must be >= 1", rect.width >= 1)
        assertTrue("Height must be >= 1", rect.height >= 1)
        assertTrue("Left must be >= 0", rect.left >= 0)
        assertTrue("Top must be >= 0", rect.top >= 0)
        assertTrue("Right must be <= imageWidth", rect.right <= 1)
        assertTrue("Bottom must be <= imageHeight", rect.bottom <= 1)
    }
}
