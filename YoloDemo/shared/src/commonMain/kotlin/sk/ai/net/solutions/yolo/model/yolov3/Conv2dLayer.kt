package sk.ai.net.solutions.yolo.model.yolov3

/**
 * Represents a convolutional layer in the YOLOv3-tiny model.
 *
 * @property weights The weights tensor for the convolutional layer
 * @property biases The biases tensor for the convolutional layer
 * @property scales The scales tensor for batch normalization (null if batch normalization is not used)
 * @property rollingMean The rolling mean tensor for batch normalization (null if batch normalization is not used)
 * @property rollingVariance The rolling variance tensor for batch normalization (null if batch normalization is not used)
 * @property padding The padding value for the convolutional layer (default: 1)
 * @property batchNormalize Whether to use batch normalization (default: true)
 * @property activate Whether to use activation (leaky ReLU) (default: true)
 */
data class Conv2dLayer(
    val weights: Any,
    val biases: Any,
    val scales: Any? = null,
    val rollingMean: Any? = null,
    val rollingVariance: Any? = null,
    val padding: Int = 1,
    val batchNormalize: Boolean = true,
    val activate: Boolean = true
)
