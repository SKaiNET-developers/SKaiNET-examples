package com.kkon.kmp.mnist.application.classifier

import com.kkon.kmp.mnist.data.image.GrayScale28To28Image

/**
 * Interface for digit classification models.
 * 
 * This interface defines the contract for any model that can classify
 * handwritten digits from grayscale images.
 */
interface DigitClassifier {
    /**
     * Classifies a grayscale image and returns the predicted digit.
     * 
     * @param image The grayscale image to classify
     * @return The predicted digit (0-9)
     */
    fun classify(image: GrayScale28To28Image): Int
    
    /**
     * Loads the model weights and parameters.
     * This should be called before using the classifier.
     */
    suspend fun loadModel()
}