package com.kkon.kmp.mnist.demo

import kotlinx.io.Source
import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.io.csv.CsvParametersLoader
import sk.ai.net.io.mapper.NamesBasedValuesModelMapper
import sk.ai.net.nn.reflection.flattenParams
import sk.ai.net.nn.reflection.summary


class ADigitClassifier(private val handleSource: () -> Source) : DigitClassifier {

    private val model = DigitClassifierNN()

    override fun classify(image: DigitClassifier.GrayScale28To28Image): Int {
        // Convert the 28x28 image to a flattened array of 784 values
        val flattenedPixels = DoubleArray(28 * 28)
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                flattenedPixels[y * 28 + x] = image.getPixel(x, y).toDouble()
            }
        }

        // Create a tensor from the flattened pixels
        val inputTensor = DoublesTensor(Shape(1, 784), flattenedPixels)

        // Forward pass through the model
        val outputTensor = model.forward(inputTensor) as DoublesTensor

        // Find the index of the maximum value in the output tensor
        // This corresponds to the predicted digit (0-9)
        var maxIndex = 0
        var maxValue = outputTensor[0]

        // The output tensor should have 10 elements (one for each digit 0-9)
        for (i in 1 until 10) {
            val value = outputTensor[i]
            if (value > maxValue) {
                maxValue = value
                maxIndex = i
            }
        }

        return maxIndex
    }

    override suspend fun loadModel() {
        try {
            println("Starting to load model...")
            println(model.summary(Shape(128, 784)))

            println("Creating CsvParametersLoader...")
            val source = handleSource()
            println("Source obtained: $source")
            val parametersLoader = CsvParametersLoader(handleSource)

            println("Creating NamesBasedValuesModelMapper...")
            val mapper = NamesBasedValuesModelMapper()
            println(model.summary(Shape(128, 784)))

            println("Loading parameters...")
            parametersLoader.load { name, shape ->
                println("Mapping parameter: $name")
                mapper.mapToModel(model, mapOf(name to shape))
            }

            println("Getting flattened parameters...")
            val params = flattenParams(model)
            println("Model loaded successfully")
            println(params)
        } catch (e: Exception) {
            println("Error loading model: ${e.message ?: "Unknown error"}")
            println("Exception details: ${e.toString()}")
            e.printStackTrace()
            throw e  // Re-throw the exception to propagate it to the caller
        }
    }
}
