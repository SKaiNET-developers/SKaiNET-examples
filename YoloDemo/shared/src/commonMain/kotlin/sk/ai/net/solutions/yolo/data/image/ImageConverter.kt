package sk.ai.net.solutions.yolo.data.image

/**
 * Utility functions for converting images to tensors.
 */

/**
 * Converts a YoloImage to a tensor for input to the YOLO model.
 *
 * @param image The image to convert
 * @return The tensor representation of the image
 */
fun convertImageToTensor(image: YoloImage): Any {
    // In a real implementation, this would convert the image to a tensor
    // For now, we'll just return a placeholder
    return object {}
}

/**
 * Normalizes the pixel values of an image.
 *
 * @param image The image to normalize
 * @return A new image with normalized pixel values
 */
fun normalizeImage(image: YoloImage): YoloImage {
    val result = YoloImage(image.width, image.height, image.channels)
    
    // Normalize the pixel values to the range [0, 1]
    for (i in image.data.indices) {
        result.data[i] = image.data[i] / 255.0f
    }
    
    return result
}