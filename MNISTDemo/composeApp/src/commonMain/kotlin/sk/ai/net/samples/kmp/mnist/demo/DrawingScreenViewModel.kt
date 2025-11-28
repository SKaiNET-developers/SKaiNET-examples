package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkon.kmp.mnist.demo.ADigitClassifier
import com.kkon.kmp.mnist.demo.DigitClassifier
import kotlinx.coroutines.launch
import kotlinx.io.Source

class DrawingScreenViewModel(handleSource: () -> Source) : ViewModel() {

    // Get the classifier from the singleton
    private val handleSourceFn = handleSource

    // Screen mode states
    var isModelLoaded by mutableStateOf(DigitClassifierSingleton.isModelLoaded)
        private set

    init {
        // Update isModelLoaded whenever DigitClassifierSingleton.isModelLoaded changes
        viewModelScope.launch {
            // This is a simple polling mechanism to check for changes
            // In a real app, you might want to use a StateFlow or other reactive approach
            while (true) {
                val newValue = DigitClassifierSingleton.isModelLoaded
                if (isModelLoaded != newValue) {
                    isModelLoaded = newValue
                }
                kotlinx.coroutines.delay(100) // Check every 100ms
            }
        }
    }
    var isChooseImageMode by mutableStateOf(false)
        private set
    var selectedImageIndex by mutableStateOf(-1)
        private set
    var classificationResult by mutableStateOf<String?>(null)
        private set

    // Drawing states
    var paths by mutableStateOf(listOf<Path>())
        private set
    var currentPath by mutableStateOf<Path?>(null)
        private set
    var currentPathRef by mutableStateOf(1)
        private set
    var lastOffset by mutableStateOf<Offset?>(null)
        private set

    // Load the model
    fun loadModel() {
        // Initialize the classifier if needed
        DigitClassifierSingleton.getClassifier(handleSourceFn)

        // Load the model if needed
        DigitClassifierSingleton.loadModelIfNeeded {
            // Update our local state when loading is complete
            isModelLoaded = DigitClassifierSingleton.isModelLoaded
        }
    }

    // Switch between drawing and image selection modes
    fun switchMode() {
        isChooseImageMode = !isChooseImageMode

        if (isChooseImageMode) {
            selectedImageIndex = -1
        } else {
            clearCanvas()
        }

        classificationResult = null
    }

    // Select an image from the grid
    fun selectImage(index: Int) {
        selectedImageIndex = index
        classificationResult = null
    }

    // Clear the drawing canvas
    fun clearCanvas() {
        paths = emptyList()
        currentPath = null
        currentPathRef = 0
        lastOffset = null
        classificationResult = null
    }

    // Handle drag start event
    fun onDragStart(offset: Offset) {
        currentPath = Path()
        currentPath?.moveTo(offset.x, offset.y)
        currentPathRef += 1
        lastOffset = offset
    }

    // Handle drag event
    fun onDrag(pointerInputChange: PointerInputChange, offset: Offset) {
        if (lastOffset != null) {
            val newOffset = Offset(
                lastOffset!!.x + offset.x,
                lastOffset!!.y + offset.y
            )
            currentPath?.lineTo(newOffset.x, newOffset.y)
            currentPathRef += 1
            lastOffset = newOffset
        }
    }

    // Handle drag end event
    fun onDragEnd() {
        currentPath.let { value ->
            if (value != null) {
                paths = paths + value
                currentPath = null
                currentPathRef = 0
            }
        }
    }

    // Classify the drawn digit or selected image
    fun classify(image: DigitClassifier.GrayScale28To28Image) {
        if (!isModelLoaded) return

        viewModelScope.launch {
            try {
                // Classify the image using the singleton
                val result = DigitClassifierSingleton.classify(image)
                if (result != null) {
                    classificationResult = "Predicted digit: $result"
                } else {
                    classificationResult = "Classification error: Model not initialized"
                    println("Classification error: Model not initialized")
                }
            } catch (e: Exception) {
                classificationResult = "Classification error: ${e.message}"
                println("Classification error: ${e.message}")
            }
        }
    }

    // Process an ImageBitmap into a GrayScale28To28Image
    private fun processImageBitmap(bitmap: ImageBitmap, output: DigitClassifier.GrayScale28To28Image) {
        // For now, we'll use a simplified approach
        // In a real implementation, we would extract pixel data from the bitmap
        // and convert it to grayscale

        // Fill with a placeholder pattern for testing
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                // Create a simple pattern (a diagonal line)
                val value = if (x == y || x == 27 - y) 1.0f else 0.0f
                output.setPixel(x, y, value)
            }
        }

        // In the future, implement proper image processing
        // This would involve:
        // 1. Getting pixel data from the bitmap
        // 2. Converting to grayscale
        // 3. Scaling to 28x28
        // 4. Normalizing values between 0 and 1
    }
}
