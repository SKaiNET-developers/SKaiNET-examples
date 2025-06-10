package com.kkon.kmp.mnist.data.image

import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.io.data.mnist.MNISTConstants

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
fun convertImageToTensor(image: GrayScale28To28Image): Tensor {
    // Get the flattened image data
    val imageData = image.data
    
    // Create a tensor with shape [784] (flattened 28x28 image)
    return DoublesTensor(
        Shape(MNISTConstants.IMAGE_PIXELS),
        imageData
    )
}