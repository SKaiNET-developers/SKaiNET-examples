package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kkon.kmp.mnist.demo.ADigitClassifier
import com.kkon.kmp.mnist.demo.DigitClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.Source

/**
 * Singleton for the DigitClassifier to ensure it's only created once
 * and the model is loaded only once.
 */
object DigitClassifierSingleton {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // State to track if the model is loaded
    var isModelLoaded by mutableStateOf(false)
        private set
    
    // The classifier instance
    private var classifier: DigitClassifier? = null
    
    /**
     * Gets or creates the classifier instance.
     * 
     * @param handleSource Function that provides the source for model weights
     * @return The classifier instance
     */
    fun getClassifier(handleSource: () -> Source): DigitClassifier {
        if (classifier == null) {
            classifier = ADigitClassifier(false, handleSource)
        }
        return classifier!!
    }
    
    /**
     * Loads the model if it's not already loaded.
     * 
     * @param onComplete Callback to be invoked when loading is complete
     */
    fun loadModelIfNeeded(onComplete: () -> Unit = {}) {
        if (isModelLoaded) {
            onComplete()
            return
        }
        
        scope.launch {
            try {
                classifier?.loadModel()
                isModelLoaded = true
                onComplete()
            } catch (e: Exception) {
                println("Error loading model: ${e.message}")
            }
        }
    }
    
    /**
     * Classifies an image using the classifier.
     * 
     * @param image The image to classify
     * @return The predicted digit or null if the classifier is not initialized
     */
    fun classify(image: DigitClassifier.GrayScale28To28Image): Int? {
        return classifier?.classify(image)
    }
}