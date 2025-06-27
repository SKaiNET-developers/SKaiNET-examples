package sk.ai.net.solutions.yolo.data.image

/**
 * Represents an image for YOLO object detection.
 *
 * @property width The width of the image in pixels
 * @property height The height of the image in pixels
 * @property channels The number of color channels (3 for RGB)
 * @property data The pixel data as a float array (values 0-1)
 */
class YoloImage(
    val width: Int,
    val height: Int,
    val channels: Int = 3,
    val data: FloatArray
) {
    /**
     * Creates a new YoloImage with the specified dimensions.
     *
     * @param width The width of the image in pixels
     * @param height The height of the image in pixels
     * @param channels The number of color channels (default: 3 for RGB)
     * @return A new YoloImage with the specified dimensions
     */
    constructor(width: Int, height: Int, channels: Int = 3) : this(
        width,
        height,
        channels,
        FloatArray(width * height * channels)
    )

    /**
     * Resizes the image to the specified dimensions while preserving aspect ratio.
     * This is known as letterboxing, where the image is resized to fit within the target dimensions
     * and the remaining space is filled with black.
     *
     * @param targetWidth The target width
     * @param targetHeight The target height
     * @return A new YoloImage with the specified dimensions
     */
    fun letterbox(targetWidth: Int, targetHeight: Int): YoloImage {
        // Calculate the scaling factor to fit the image within the target dimensions
        val scale = minOf(
            targetWidth.toFloat() / width,
            targetHeight.toFloat() / height
        )

        // Calculate the new dimensions
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        // Calculate the offset to center the image
        val offsetX = (targetWidth - newWidth) / 2
        val offsetY = (targetHeight - newHeight) / 2

        // Create a new image with the target dimensions
        val result = YoloImage(targetWidth, targetHeight, channels)

        // Resize the image and copy the pixel data
        // In a real implementation, this would use proper image scaling algorithms
        // For now, we'll just fill the result with zeros (black)
        return result
    }
}