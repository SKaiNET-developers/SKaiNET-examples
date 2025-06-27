package sk.ai.net.solutions.yolo.application.detector

/**
 * Represents a bounding box for object detection.
 *
 * @property x The x-coordinate of the center of the box (normalized 0-1)
 * @property y The y-coordinate of the center of the box (normalized 0-1)
 * @property w The width of the box (normalized 0-1)
 * @property h The height of the box (normalized 0-1)
 */
data class Box(
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float
)