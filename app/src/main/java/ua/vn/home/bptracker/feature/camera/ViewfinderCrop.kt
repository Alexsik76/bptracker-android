package ua.vn.home.bptracker.feature.camera

import kotlin.math.roundToInt

data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
}

/**
 * Calculates the crop rectangle in image coordinates for the centered viewfinder frame with margin.
 *
 * @param imageWidth Upright image width in pixels.
 * @param imageHeight Upright image height in pixels.
 * @param viewWidthPx PreviewView width in pixels.
 * @param viewHeightPx PreviewView height in pixels.
 * @param frameWidthPx Viewfinder frame width in pixels.
 * @param frameHeightPx Viewfinder frame height in pixels.
 * @param margin Margin expansion factor applied to each side of the frame (e.g. 0.10f for 10%).
 * @return A non-empty [CropRect] clamped to the image boundaries.
 */
fun viewfinderCropRect(
    imageWidth: Int,
    imageHeight: Int,
    viewWidthPx: Float,
    viewHeightPx: Float,
    frameWidthPx: Float,
    frameHeightPx: Float,
    margin: Float,
): CropRect {
    if (viewWidthPx <= 0f || viewHeightPx <= 0f || imageWidth <= 0 || imageHeight <= 0) {
        return CropRect(
            left = 0,
            top = 0,
            width = maxOf(1, imageWidth),
            height = maxOf(1, imageHeight),
        )
    }

    val scaleX = imageWidth.toFloat() / viewWidthPx
    val scaleY = imageHeight.toFloat() / viewHeightPx

    val expandedWidthPx = (frameWidthPx * (1f + 2f * margin)).coerceAtLeast(0f)
    val expandedHeightPx = (frameHeightPx * (1f + 2f * margin)).coerceAtLeast(0f)

    val cropWidthInImage = expandedWidthPx * scaleX
    val cropHeightInImage = expandedHeightPx * scaleY

    val rawLeft = (imageWidth.toFloat() - cropWidthInImage) / 2f
    val rawRight = rawLeft + cropWidthInImage
    val rawTop = (imageHeight.toFloat() - cropHeightInImage) / 2f
    val rawBottom = rawTop + cropHeightInImage

    val clampedLeft = rawLeft.coerceIn(0f, imageWidth.toFloat())
    val clampedRight = rawRight.coerceIn(0f, imageWidth.toFloat())
    val clampedTop = rawTop.coerceIn(0f, imageHeight.toFloat())
    val clampedBottom = rawBottom.coerceIn(0f, imageHeight.toFloat())

    var left = clampedLeft.roundToInt()
    var right = clampedRight.roundToInt()
    var top = clampedTop.roundToInt()
    var bottom = clampedBottom.roundToInt()

    if (right <= left) {
        if (left < imageWidth) {
            right = left + 1
        } else {
            left = maxOf(0, imageWidth - 1)
            right = imageWidth
        }
    }

    if (bottom <= top) {
        if (top < imageHeight) {
            bottom = top + 1
        } else {
            top = maxOf(0, imageHeight - 1)
            bottom = imageHeight
        }
    }

    return CropRect(
        left = left,
        top = top,
        width = right - left,
        height = bottom - top,
    )
}
