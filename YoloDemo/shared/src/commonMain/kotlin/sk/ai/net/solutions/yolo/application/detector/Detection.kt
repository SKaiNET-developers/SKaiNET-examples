package sk.ai.net.solutions.yolo.application.detector

/**
 * Represents a detected object with a bounding box, class probabilities, and objectness score.
 *
 * @property bbox The bounding box of the detected object
 * @property prob The probabilities for each class (0-1)
 * @property objectness The objectness score (0-1)
 */
data class Detection(
    val bbox: Box,
    val prob: List<Float>,
    val objectness: Float
)