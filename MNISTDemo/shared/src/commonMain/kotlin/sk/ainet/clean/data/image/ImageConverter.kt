package sk.ainet.clean.data.image

import sk.ainet.context.ExecutionContext
import sk.ainet.context.data
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.dsl.*
import sk.ainet.lang.types.FP32


/**
 * Utility functions for converting MNIST images to tensors for neural network processing.
 */

/**
 * Converts a MNIST image to a tensor suitable for input to a neural network.
 *
 * The MNIST image is a 28x28 grayscale image.
 * This function:
 * 1. Takes the already normalized pixel values (0-1)
 * 2. Flattens the 28x28 image into a 784-element vector
 * 3. Creates a tensor with shape [784]
 *
 * @param image The MNIST image to convert.
 * @return A tensor representing the image.
 */
fun convertImageToTensor(context: ExecutionContext, image: GrayScale28To28Image): Tensor<FP32, Float> {
    // Get the flattened image data
    val imageData = image.data

    return data<FP32, Float>(context) {
        tensor<FP32, Float> {
            shape(1, 1, 28, 28) {
                fromArray(imageData.map { it.toFloat() }.toFloatArray())
            }
        }
    }
}