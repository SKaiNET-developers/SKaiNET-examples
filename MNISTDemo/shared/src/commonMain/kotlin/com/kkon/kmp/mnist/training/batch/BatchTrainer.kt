package com.kkon.kmp.mnist.training.batch

import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Module

/**
 * Utility functions and classes for batch training of neural networks on the MNIST dataset.
 */

/**
 * Flattens a list of DoubleArray into a single DoubleArray.
 * 
 * @param arrays List of DoubleArray to flatten
 * @return A single DoubleArray containing all elements from the input arrays
 */
fun flattenDoubleArrays(arrays: List<DoubleArray>): DoubleArray {
    // Calculate the total size of the flattened array
    val totalSize = arrays.sumOf { it.size }

    // Create a new array of the total size
    val result = DoubleArray(totalSize)

    // Copy all elements from the input arrays to the result array
    var index = 0
    for (array in arrays) {
        for (element in array) {
            result[index++] = element
        }
    }

    return result
}

/**
 * Creates batches from MNIST dataset for training.
 * 
 * @param images List of flattened images (each as a DoubleArray of 784 values)
 * @param labels List of labels (0-9)
 * @param batchSize Size of each batch
 * @return List of pairs of input and target tensors for each batch
 */
fun createMnistBatches(
    images: List<DoubleArray>, 
    labels: List<Int>, 
    batchSize: Int
): List<Pair<Tensor, Tensor>> {
    val batches = mutableListOf<Pair<Tensor, Tensor>>()

    // Calculate number of complete batches
    val numBatches = images.size / batchSize

    for (i in 0 until numBatches) {
        val batchImages = mutableListOf<DoubleArray>()
        val batchLabels = mutableListOf<DoubleArray>()

        // Collect samples for this batch
        for (j in 0 until batchSize) {
            val index = i * batchSize + j
            batchImages.add(images[index])

            // One-hot encode the label
            val oneHotLabel = DoubleArray(10) { 0.0 }
            oneHotLabel[labels[index]] = 1.0
            batchLabels.add(oneHotLabel)
        }

        // Create tensors for this batch
        val inputTensor = DoublesTensor(
            Shape(batchSize, 784), 
            flattenDoubleArrays(batchImages)
        )
        val targetTensor = DoublesTensor(
            Shape(batchSize, 10), 
            flattenDoubleArrays(batchLabels)
        )

        batches.add(Pair(inputTensor, targetTensor))
    }

    return batches
}

/**
 * Evaluates a model on the MNIST test dataset.
 * 
 * @param model The neural network model to evaluate
 * @param testImages List of flattened test images
 * @param testLabels List of test labels
 * @return Accuracy as a percentage
 */
fun evaluateModel(
    model: Module,
    testImages: List<DoubleArray>,
    testLabels: List<Int>
): Double {
    var correct = 0

    for (i in testImages.indices) {
        // Create a tensor for a single image
        val inputTensor = DoublesTensor(
            Shape(1, 784),
            testImages[i]
        )

        // Forward pass
        val outputTensor = model.forward(inputTensor) as DoublesTensor
        val outputData = outputTensor.elements

        // Find the predicted class (index of maximum value)
        var maxIndex = 0
        var maxValue = outputData[0]
        for (j in 1 until 10) {
            if (outputData[j] > maxValue) {
                maxValue = outputData[j]
                maxIndex = j
            }
        }

        // Check if prediction is correct
        if (maxIndex == testLabels[i]) {
            correct++
        }
    }

    // Calculate accuracy
    return (correct.toDouble() / testImages.size) * 100.0
}