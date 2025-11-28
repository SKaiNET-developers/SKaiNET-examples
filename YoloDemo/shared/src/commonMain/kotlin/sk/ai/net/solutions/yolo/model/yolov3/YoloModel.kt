package sk.ai.net.solutions.yolo.model.yolov3

/**
 * Represents the YOLOv3-tiny model.
 *
 * @property width The width of the input image (default: 416)
 * @property height The height of the input image (default: 416)
 * @property conv2dLayers The convolutional layers of the model
 * @property backend The backend used for computation
 */
data class YoloModel(
    val width: Int = 416,
    val height: Int = 416,
    val conv2dLayers: List<Conv2dLayer>,
    val backend: Any
)