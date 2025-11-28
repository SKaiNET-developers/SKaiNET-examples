package com.kkon.kmp.mnist.model.common

import com.kkon.kmp.mnist.data.image.convertImageToTensor
import com.kkon.kmp.mnist.data.image.GrayScale28To28Image
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Module

/**
 * This file contains functions for classifying MNIST images using neural networks.
 */

/**
 * Classifies a MNIST image using the given neural network.
 *
 * This function:
 * 1. Converts the MNIST image to a tensor
 * 2. Passes the tensor through the neural network
 * 3. Finds the index of the maximum value in the output tensor
 * 4. Returns the index as the predicted digit (0-9)
 *
 * The output tensor has 10 elements, one for each possible digit.
 * The element with the highest value corresponds to the most likely digit.
 *
 * @param model The neural network to use for classification.
 * @param image The MNIST image to classify.
 * @return The predicted digit (0-9).
 */
fun classifyImage(model: Module, image: GrayScale28To28Image): Int {
    // Convert the image to a tensor
    val inputTensor = convertImageToTensor(image)

    // Forward pass through the network
    val outputTensor = model.forward(inputTensor) as DoublesTensor

    // Find the index of the maximum value in the output tensor
    // This is the predicted digit
    val outputElements = outputTensor.elements
    var maxIndex = 0
    var maxValue: Double = outputElements[0]

    for (i in 1 until outputElements.size) {
        if (outputElements[i] > maxValue) {
            maxValue = outputElements[i]
            maxIndex = i
        }
    }

    return maxIndex
}
