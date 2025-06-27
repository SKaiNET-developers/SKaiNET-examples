package sk.ai.net.solutions.yolo.model.yolov3

/**
 * Represents a YOLO detection layer.
 *
 * @property classes The number of classes (default: 80)
 * @property mask The mask indices for the anchors
 * @property anchors The anchor box dimensions
 * @property predictions The predictions from the model
 * @property w The width of the feature map
 * @property h The height of the feature map
 */
class YoloLayer(
    val classes: Int = 80,
    val mask: List<Int>,
    val anchors: List<Float>,
    val predictions: FloatArray
) {
    val w: Int
    val h: Int

    init {
        // In a real implementation, these would be set based on the shape of the predictions tensor
        // For now, we'll just set them to default values
        w = 13
        h = 13
    }

    /**
     * Calculates the index in the predictions array for a given location and entry.
     *
     * @param location The location index
     * @param entry The entry index
     * @return The index in the predictions array
     */
    fun entryIndex(location: Int, entry: Int): Int {
        val n = location / (w * h)
        val loc = location % (w * h)
        return n * w * h * (4 + classes + 1) + entry * w * h + loc
    }
}