package com.kkon.kmp.mnist.application.classifier

import com.kkon.kmp.mnist.data.image.GrayScale28To28Image
import com.kkon.kmp.mnist.data.loader.loadModelWeights
import com.kkon.kmp.mnist.model.cnn.createMNISTCNN
import com.kkon.kmp.mnist.model.common.classifyImage
import com.kkon.kmp.mnist.model.mlp.createMLP
import kotlinx.io.Source
import sk.ai.net.nn.Module

/**
 * Implementation of the DigitClassifier interface using neural networks.
 * 
 * This class can use either a Convolutional Neural Network (CNN) or a
 * Multi-Layer Perceptron (MLP) for digit classification.
 * 
 * @param useCNN Whether to use a CNN (true) or MLP (false) for classification
 * @param handleSource Function that provides the source for model weights
 */
class NeuralNetworkDigitClassifier(
    useCNN: Boolean = false, 
    private val handleSource: () -> Source
) : DigitClassifier {

    /**
     * The neural network model used for classification.
     * This is either a CNN or an MLP, depending on the useCNN parameter.
     */
    val model: Module = if (useCNN) createMNISTCNN() else createMLP()

    /**
     * Classifies a grayscale image and returns the predicted digit.
     * 
     * @param image The grayscale image to classify
     * @return The predicted digit (0-9)
     */
    override fun classify(image: GrayScale28To28Image): Int {
        return classifyImage(model, image)
    }

    /**
     * Loads the model weights from the provided source.
     * This should be called before using the classifier.
     */
    override suspend fun loadModel() {
        handleSource().use { source ->
            try {
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