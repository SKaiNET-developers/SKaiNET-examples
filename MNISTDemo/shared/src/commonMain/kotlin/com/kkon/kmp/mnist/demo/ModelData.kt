package com.kkon.kmp.mnist.demo

import com.kkon.kmp.mnist.nn.MLPFactory
import com.kkon.kmp.mnist.nn.classifyImage
import com.kkon.kmp.mnist.nn.createMNISTCNN
import com.kkon.kmp.mnist.nn.loadModelWeights
import kotlinx.io.Source

class ADigitClassifier(useCNN: Boolean = false, private val handleSource: () -> Source) : DigitClassifier {

    val model = if (useCNN) createMNISTCNN() else MLPFactory()

    override fun classify(image: DigitClassifier.GrayScale28To28Image): Int {
        // Use the new 'of' extension function to get the output tensor
        return classifyImage(model, image)
    }

    override suspend fun loadModel() {
        handleSource().use { source ->
            try {
                // Create a GGUFReader to parse the file
                loadModelWeights(model, source)
            } catch (e: Exception) {
                println("Error loading model: ${e.message ?: "Unknown error"}")
                println("Exception details: ${e.toString()}")
                e.printStackTrace()
                throw e  // Re-throw the exception to propagate it to the caller
            }
        }
    }
}
